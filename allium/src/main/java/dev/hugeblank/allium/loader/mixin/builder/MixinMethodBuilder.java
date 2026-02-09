package dev.hugeblank.allium.loader.mixin.builder;

import dev.hugeblank.allium.api.event.MixinMethodHook;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotationParser;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaCancellable;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaSugar;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedElement;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class MixinMethodBuilder {
    private final ClassWriter classWriter;
    private final VisitedElement target;
    private int access = 0;
    private final String name;
    private final List<MixinParameter> initialParameters;
    private final List<MixinParameter> additionalParameters = new ArrayList<>();
    private List<LuaAnnotationParser> methodAnnotations;
    private String signature = null;
    private final List<String> exceptions = new ArrayList<>();
    private WriteFactory code;
    private Type returnType = Type.VOID_TYPE;

    private MixinMethodBuilder(ClassWriter classWriter, VisitedElement target, String name, List<MixinParameter> initialParameters) {
        this.classWriter = classWriter;
        this.target = target;
        this.name = name;
        this.initialParameters = new ArrayList<>(initialParameters);
    }

    public static MixinMethodBuilder of(ClassWriter classWriter, VisitedElement target, String name, List<MixinParameter> initialParameters) {
        return new MixinMethodBuilder(classWriter, target, name, initialParameters);
    }

    public MixinMethodBuilder access(int access) {
        this.access = access;
        return this;
    }

    public MixinMethodBuilder returnType(Type newReturnType) {
        returnType = newReturnType;
        return this;
    }

    public MixinMethodBuilder parameter(MixinParameter parameter) {
        additionalParameters.add(parameter);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public MixinMethodBuilder sugars(List<? extends LuaSugar> luaSugars) throws InvalidArgumentException {
        if (!luaSugars.isEmpty()) {
            for (LuaSugar ls : luaSugars) {
                if (ls instanceof LuaCancellable lc) {
                    if (target instanceof VisitedMethod targetMethod) {
                        lc.methodIsReturnable(!Type.getReturnType(targetMethod.descriptor()).equals(Type.VOID_TYPE));
                    } else {
                        throw new InvalidArgumentException(
                                "Cancellable parameter cannot be applied to field-based mixin methods."
                        );
                    }
                }
                if (ls instanceof LuaParameterAnnotation lp) {
                    parameter(new MixinParameter(
                            Type.getType(ls.type()), List.of(lp.luaAnnotation())
                    ));
                }
            }
        }
        return this;
    }

    public MixinMethodBuilder code(WriteFactory methodWriteFactory) {
        this.code = methodWriteFactory;
        return this;
    }

    public MixinMethodBuilder annotations(List<LuaAnnotationParser> annotations) {
        methodAnnotations = annotations;
        return this;
    }

    public MixinMethodBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public MixinMethodBuilder exceptions(String[] exceptions) {
        if (exceptions != null) {
            this.exceptions.addAll(Arrays.asList(exceptions));
        }
        return this;
    }

    public void build() throws InvalidArgumentException, InvalidMixinException, LuaError {
        build(null, null);
    }

    public void build(@Nullable Script script, @Nullable String id) throws LuaError, InvalidArgumentException, InvalidMixinException {

        List<MixinParameter> params = new ArrayList<>(initialParameters);

        params.addAll(additionalParameters);

        String descriptor = Type.getMethodDescriptor(returnType, params.stream().map(MixinParameter::getType).toArray(Type[]::new));
        final MethodVisitor methodVisitor = classWriter.visitMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions.toArray(String[]::new)
        );

        if (methodAnnotations != null) {
            for (LuaAnnotationParser annotation : methodAnnotations) {
                Class<?> cAnnotation = annotation.type().raw();
                EClass<?> eAnnotation = EClass.fromJava(cAnnotation);
                AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotation(
                        cAnnotation.descriptorString(),
                        !eAnnotation.hasAnnotation(Retention.class) ||
                                eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
                );
                annotation.apply(annotationVisitor);
                annotationVisitor.visitEnd();
            }
        }

        for (int i = 0; i < params.size(); i++) {
            params.get(i).annotate(methodVisitor, i);
        }

        if ((access & ACC_STATIC) == 0) {
            params.addFirst(new MixinParameter(target.owner().getType()));
        }

        if (code != null) {
            methodVisitor.visitCode();
            code.write(methodVisitor, descriptor, params);
        }

        methodVisitor.visitEnd();
        if (id != null) MixinMethodHook.create(script, id, params.stream().map(MixinParameter::getType).toList(), returnType);
    }

    @FunctionalInterface
    public interface WriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, List<MixinParameter> parameters);
    }

}

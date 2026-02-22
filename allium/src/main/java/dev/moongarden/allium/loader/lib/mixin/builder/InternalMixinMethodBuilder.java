package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.LuaAnnotationParser;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaCancellable;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.VisitedElement;
import dev.moongarden.allium.util.asm.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
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

public class InternalMixinMethodBuilder {
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

    public InternalMixinMethodBuilder(ClassWriter classWriter, VisitedElement target, String name, List<MixinParameter> initialParameters) {
        this.classWriter = classWriter;
        this.target = target;
        this.name = name;
        this.initialParameters = new ArrayList<>(initialParameters);
    }

    public InternalMixinMethodBuilder access(int access) {
        this.access = access;
        return this;
    }

    public InternalMixinMethodBuilder returnType(Type newReturnType) {
        returnType = newReturnType;
        return this;
    }

    public InternalMixinMethodBuilder parameter(MixinParameter parameter) {
        additionalParameters.add(parameter);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public InternalMixinMethodBuilder sugars(List<? extends LuaSugar> luaSugars) throws InvalidMixinException {
        if (!luaSugars.isEmpty()) {
            for (LuaSugar ls : luaSugars) {
                if (ls instanceof LuaCancellable lc) {
                    if (target instanceof VisitedMethod targetMethod) {
                        lc.methodIsReturnable(!Type.getReturnType(targetMethod.descriptor()).equals(Type.VOID_TYPE));
                    } else {
                        throw new InvalidMixinException(
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

    public InternalMixinMethodBuilder code(WriteFactory methodWriteFactory) {
        this.code = methodWriteFactory;
        return this;
    }

    public InternalMixinMethodBuilder annotations(List<LuaAnnotationParser> annotations) {
        methodAnnotations = annotations;
        return this;
    }

    public InternalMixinMethodBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public InternalMixinMethodBuilder exceptions(String[] exceptions) {
        if (exceptions != null) {
            this.exceptions.addAll(Arrays.asList(exceptions));
        }
        return this;
    }

    private MethodInfo writeMethod() throws LuaError {
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

        return new MethodInfo(params.stream().map(MixinParameter::getType).toList(), returnType);
    }

    public MixinMethodHook buildForClass(Script script, String id) throws LuaError {
        MethodInfo info = writeMethod();
        return new MixinMethodHook(script, id, info.params(), info.returnType());
    }

    public void buildForInterface() throws LuaError {
        writeMethod();
    }

    @FunctionalInterface
    public interface WriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, List<MixinParameter> parameters);
    }


    private record MethodInfo(List<Type> params, Type returnType) {}
}

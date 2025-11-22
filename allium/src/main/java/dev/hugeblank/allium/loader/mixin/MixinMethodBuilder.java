package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaInjectorAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaCancellable;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.VisitedElement;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

@SuppressWarnings("UnusedReturnValue")
public class MixinMethodBuilder {
    private final ClassWriter classWriter;
    private final VisitedElement target;
    private int access = 0;
    private final String name;
    private final List<MixinParameter> initialParameters;
    private final List<MixinParameter> additionalParameters = new ArrayList<>();
    private List<LuaAnnotation> methodAnnotations;
    private String signature = null;
    private String[] exceptions = null;
    private LuaInjectorAnnotation.MethodWriteFactory code;
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

    public MixinMethodBuilder luaParameters(List<LuaParameterAnnotation> luaParams) throws InvalidArgumentException {
        if (!luaParams.isEmpty()) {
            for (LuaParameterAnnotation lp : luaParams) {
                if (lp instanceof LuaCancellable lc) {
                    if (target instanceof VisitedMethod targetMethod) {
                        lc.methodIsReturnable(!Type.getReturnType(targetMethod.descriptor()).equals(Type.VOID_TYPE));
                    } else {
                        throw new InvalidArgumentException(
                                "Cancellable parameter cannot be applied to field-based mixin methods."
                        );
                    }
                }
                parameter(new MixinParameter(
                        Type.getType(lp.type()), List.of(lp.luaAnnotation())
                ));
            }
        }
        return this;
    }

    public MixinMethodBuilder code(LuaInjectorAnnotation.MethodWriteFactory methodWriteFactory) {
        this.code = methodWriteFactory;
        return this;
    }

    public MixinMethodBuilder annotations(List<LuaAnnotation> annotations) {
        methodAnnotations = annotations;
        return this;
    }

    public MixinMethodBuilder signature(String signature) {
        this.signature = signature;
        return this;
    }

    public MixinMethodBuilder exceptions(String[] exceptions) {
        this.exceptions = exceptions;
        return this;
    }

    public InvocationReference build() throws LuaError, InvalidArgumentException, InvalidMixinException {

        List<MixinParameter> params = new ArrayList<>(initialParameters);

        params.addAll(additionalParameters);

        String descriptor = Type.getMethodDescriptor(returnType, params.stream().map(MixinParameter::getType).toArray(Type[]::new));
        final MethodVisitor methodVisitor = classWriter.visitMethod(access, name, descriptor, signature, exceptions);

        if (methodAnnotations != null) {
            for (LuaAnnotation annotation : methodAnnotations) {
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
            code.write(methodVisitor, descriptor, params, access);
        }

        methodVisitor.visitEnd();
        return new InvocationReference(params.stream().map(MixinParameter::getType).toList());
    }

    public record InvocationReference(List<Type> paramTypes) {
        public void createEvent(String id) {
            new MixinEventType(id, paramTypes.stream().map(AsmUtil::getWrappedTypeName).toList());
        }
    }

}

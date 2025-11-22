package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.method.InjectorChef;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.method.MixinMethodAnnotations;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotation;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.Registry;
import dev.hugeblank.allium.util.asm.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/* Notes on implementation:
 - Accessors & Invokers MUST have target name set as the value in the annotation. We use that to determine which
 method/field to invoke/access
*/

@LuaWrapped
public class MixinClassBuilder {
    public static final Registry<MixinClassInfo> MIXINS = new Registry<>();
    public static final Registry<MixinClassInfo> CLIENT = new Registry<>();
    public static final Registry<MixinClassInfo> SERVER = new Registry<>();

    private final String className = AsmUtil.getUniqueMixinClassName();
    private final EnvType targetEnvironment;
    private final boolean duck;
    private final VisitedClass visitedClass;
    private final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final Script script;

    public MixinClassBuilder(String target, String[] interfaces, @Nullable EnvType targetEnvironment, boolean duck, Script script) throws LuaError {
        checkPhase();
        Allium.PROFILER.push(script.getID(), "mixin", target);
        this.script = script;
        this.targetEnvironment = targetEnvironment;
        this.duck = duck;
        this.visitedClass = VisitedClass.ofClass(target);

        EClass<?> superClass = EClass.fromJava(Object.class);
        List<String> ifaces = Arrays.asList(interfaces);
        this.c.visit(
                V17,
                ACC_PUBLIC | visitedClass.access(),
                className,
                visitedClass.signature(),
                superClass.name().replace('.', '/'),
                ifaces.toArray(String[]::new)
        );

        AnnotationVisitor mixinAnnotation = this.c.visitAnnotation(Mixin.class.descriptorString(), false);
        AnnotationVisitor targetArray = mixinAnnotation.visitArray("value");
        targetArray.visit(null, Type.getObjectType(visitedClass.name()));
        targetArray.visitEnd();
        mixinAnnotation.visitEnd();
        Allium.PROFILER.pop();
    }

    @LuaWrapped
    public void createInjectMethod(String eventName, List<LuaMethodAnnotation> methodAnnotations, @OptionalArg @Nullable List<LuaParameterAnnotation> sugarParameters) throws InvalidMixinException, InvalidArgumentException, LuaError {
        checkPhase();
        Allium.PROFILER.push("createInjectMethod", eventName);
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");

        String eventId = script.getID() + ':' + eventName;

        List<InjectorChef> chefList = methodAnnotations.stream()
                .filter((methodAnnotation) -> methodAnnotation instanceof InjectorChef)
                .map(ma -> (InjectorChef) ma).toList();
        if (chefList.isEmpty()) {
            throw new InvalidMixinException(InvalidMixinException.Type.NO_INJECTOR_ANNOTATION, eventId);
        } else if (chefList.size() > 1) {
            throw new InvalidMixinException(InvalidMixinException.Type.TOO_MANY_INJECTOR_ANNOTATIONS, eventId);
        }
        chefList.getFirst().bake(script, eventId, c, visitedClass, methodAnnotations, sugarParameters);
        Allium.PROFILER.pop();
    }

    @LuaWrapped
    public void accessor(LuaTable annotations) throws InvalidArgumentException, InvalidMixinException, LuaError {
        // Shorthand method for writing both setter and getter accessor methods
        setAccessor(annotations);
        getAccessor(annotations);
    }

    @LuaWrapped
    public void setAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(true, annotations);
    }

    @LuaWrapped
    public void getAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(false, annotations);
    }

    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String fieldName = getTargetValue(annotations);
        if (visitedClass.containsField(fieldName)) {
            VisitedField visitedField = visitedClass.getField(fieldName);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            String name = visitedField.name();
            name = (isSetter ? "set" : "get") + // set or get
                    name.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    name.substring(1); // Rest of name
            List<MixinParameter> params = isSetter ? List.of(new MixinParameter(visitedFieldType)) : List.of();

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedField, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotation(
                    script.getExecutor().getState(),
                    annotations,
                    EClass.fromJava(Accessor.class)
            )));

            if (visitedField.needsInstance()) {
                methodBuilder.code((visitor, desc, mparams, access) -> {
                    AsmUtil.visitObjectDefinition(
                            visitor,
                            Type.getInternalName(UnsupportedOperationException.class),
                            "()V"
                    ).run();
                    visitor.visitInsn(ATHROW);
                    visitor.visitMaxs(0, 0);
                });
            }

            methodBuilder
                    .access(visitedField.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT))
                    .returnType(isSetter ? Type.VOID_TYPE : visitedFieldType)
                    .signature(visitedField.signature())
                    .build();
        }
    }


    @LuaWrapped
    public void invoker(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        checkPhase();
        String methodName = getTargetValue(annotations);
        if (visitedClass.containsMethod(methodName)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(methodName);
            String name = visitedMethod.name();
            name = "invoke" +
                    name.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    name.substring(1);// Rest of name

            List<MixinParameter> params = Arrays.stream(
                    Type.getArgumentTypes(visitedMethod.descriptor())).map(MixinParameter::new
            ).toList();

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedMethod, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotation(
                    script.getExecutor().getState(),
                    annotations,
                    EClass.fromJava(Invoker.class)
            )));

            if (visitedMethod.needsInstance()) {
                methodBuilder.code((visitor, desc, mparams, access) -> {
                    AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(AssertionError.class), "()V").run();
                    visitor.visitInsn(ATHROW);
                    visitor.visitMaxs(0,0);
                });
            }

            methodBuilder
                    .access(visitedMethod.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT))
                    .returnType(Type.getReturnType(visitedMethod.descriptor()))
                    .signature(visitedMethod.signature())
                    .exceptions(visitedMethod.exceptions())
                    .build();
        }
    }

    private String getTargetValue(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        if (!visitedClass.isInterface())
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "interface");
        String name = null;
        if (annotations.rawget("value").isString()) {
            name = annotations.rawget("value").checkString();
        } else if (annotations.rawget(1).isString()) {
            name = annotations.rawget(1).checkString();
        }
        if (name == null) {
            throw new InvalidArgumentException("Expected field name at key 'value' or index 1");
        } else {
            return name;
        }
    }


    private static void checkPhase() {
        if (MixinConfigUtil.isComplete())
            throw new IllegalStateException("Mixins cannot be created outside of preLaunch phase.");
    }

    @LuaWrapped
    public MixinClassInfo build() {
        c.visitEnd();
        byte[] classBytes = c.toByteArray();
        AsmUtil.dumpClass(className, classBytes);

        // give the class back to the user for later use in the case of an interface.
        MixinClassInfo info = new MixinClassInfo(className.replace("/", "."), classBytes, this.duck);

        Registry<MixinClassInfo> registry = (targetEnvironment == null) ? MIXINS : switch (targetEnvironment) {
            case SERVER -> SERVER;
            case CLIENT -> CLIENT;
        };
        registry.register(info);
        return info;
    }

}


package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.lib.MixinLib;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
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
    private int methodIndex = 0;
    private final Script script;

    public MixinClassBuilder(String target, String[] interfaces, @Nullable EnvType targetEnvironment, boolean duck, Script script) throws LuaError {
        checkPhase();
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
    }

    @LuaWrapped
    public void inject(String eventName, LuaTable annotations, @OptionalArg @Nullable List<MixinLib.LuaLocal> locals) throws LuaError, InvalidMixinException, InvalidArgumentException {
        checkPhase();
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");

        LuaAnnotation luaAnnotation = new LuaAnnotation(
                script.getExecutor().getState(),
                annotations,
                EClass.fromJava(Inject.class)
        );

        writeInject(eventName, luaAnnotation, locals);
    }

    @LuaWrapped
    public void modifyArgs(String eventName, LuaTable annotations, @OptionalArg @Nullable List<MixinLib.LuaLocal> locals) throws InvalidMixinException, InvalidArgumentException, LuaError {
        checkPhase();
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");

        LuaAnnotation luaAnnotation = new LuaAnnotation(
                script.getExecutor().getState(),
                annotations,
                EClass.fromJava(ModifyArgs.class)
        );

        String descriptor = luaAnnotation.findElement("method", String.class);
        if (visitedClass.containsMethod(descriptor)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(descriptor);

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                    c, visitedClass,
                    createInjectName(visitedMethod.name()),
                    List.of(new MixinParameter(Type.getType(Args.class)))
            );

            if (locals != null) methodBuilder.locals(locals);

            MixinMethodBuilder.InvocationReference invocationReference = methodBuilder
                    .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                    .returnType(Type.VOID_TYPE)
                    .annotations(List.of(luaAnnotation))
                    .signature(visitedMethod.signature())
                    .exceptions(visitedMethod.exceptions())
                    .code(createInjectWriteFactory(eventName))
                    .build();

            invocationReference.createEvent(script.getID() + ":" + eventName);
        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        }
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

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedClass, name, params);

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

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedClass, name, params);

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

    private void writeInject(String eventName, LuaAnnotation annotation, @Nullable List<MixinLib.LuaLocal> locals) throws LuaError, InvalidMixinException, InvalidArgumentException {
        String descriptor = annotation.findElement("method", String.class);
        if (visitedClass.containsMethod(descriptor)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(descriptor);
            List<MixinParameter> params = new ArrayList<>(visitedMethod.getParams().stream().map(MixinParameter::new).toList());
            Type returnType = Type.getReturnType(visitedMethod.descriptor());
            if (returnType.equals(Type.VOID_TYPE)) {
                params.add(new MixinParameter(Type.getType(CallbackInfo.class)));
            } else {
                params.add(new MixinParameter(Type.getType(CallbackInfoReturnable.class)));
            }


            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                    c, visitedClass, createInjectName(visitedMethod.name()), params
            );

            if (locals != null) methodBuilder.locals(locals);

            MixinMethodBuilder.InvocationReference invocationReference = methodBuilder
                    .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                    .returnType(Type.VOID_TYPE)
                    .annotations(List.of(annotation))
                    .signature(visitedMethod.signature())
                    .exceptions(visitedMethod.exceptions())
                    .code(createInjectWriteFactory(eventName))
                    .build();

            invocationReference.createEvent(script.getID() + ":" + eventName);
        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        }
    }

    private MethodWriteFactory createInjectWriteFactory(String eventName) {
        return (methodVisitor, desc, paramTypes, access) -> {
            int varPrefix = paramTypes.size();
            List<Type> types = paramTypes.stream().map(MixinParameter::getType).toList();
            AsmUtil.createArray(methodVisitor, varPrefix, types, Object.class, (visitor, index, arg) -> {
                visitor.visitVarInsn(arg.getOpcode(ILOAD), index); // <- 2
                AsmUtil.wrapPrimitive(visitor, arg); // <- 2 | -> 2 (sometimes)
                if (index == 0) {
                    visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
                }
            }); // <- 0
            methodVisitor.visitFieldInsn(
                    GETSTATIC, Type.getInternalName(MixinEventType.class),
                    "EVENT_MAP", Type.getDescriptor(Map.class)
            ); // <- 1
            methodVisitor.visitLdcInsn(script.getID()+":"+eventName); // <- 2
            methodVisitor.visitMethodInsn(
                    INVOKEINTERFACE,
                    Type.getInternalName(Map.class),
                    "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                    true
            ); // -> 1, 2 | <- 1
            methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(MixinEventType.class)); // <- 1 | -> 1
            methodVisitor.visitInsn(SWAP); // 0 <-> 1
            methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    Type.getInternalName(MixinEventType.class),
                    "invoke",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object[].class)),
                    false
            ); // -> 0, 1

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(0, 0);
        };
    }

    private String createInjectName(String visitedMethodName) {
        return script.getID() + "$" +
                visitedMethodName
                        .replace("<", "")
                        .replace(">", "") +
                methodIndex++;
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

    @FunctionalInterface
    public interface MethodWriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, List<MixinParameter> parameters, int access) throws InvalidArgumentException, LuaError, InvalidMixinException;
    }

}


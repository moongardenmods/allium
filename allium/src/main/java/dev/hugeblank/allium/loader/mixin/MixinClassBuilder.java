package dev.hugeblank.allium.loader.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.InvalidMixinException;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.Registry;
import dev.hugeblank.allium.util.asm.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public MixinClassBuilder(String target, @Nullable EnvType targetEnvironment, boolean duck, Script script) throws LuaError {
        if (MixinConfigUtil.isComplete()) throw new IllegalStateException("Mixin cannot be created outside of preLaunch phase.");
        this.script = script;
        this.targetEnvironment = targetEnvironment;
        this.duck = duck;
        LuaState state = script.getExecutor().getState();
        this.visitedClass = VisitedClass.ofClass(state, target);

        EClass<?> superClass = EClass.fromJava(Object.class);
        this.c.visit(
                V17,
                ACC_PUBLIC | visitedClass.access(),
                className,
                null,
                superClass.name().replace('.', '/'),
                null
        );

        AnnotationVisitor mixinAnnotation = this.c.visitAnnotation(Mixin.class.descriptorString(), false);
        AnnotationVisitor targetArray = mixinAnnotation.visitArray("value");
        targetArray.visit(null, Type.getObjectType(visitedClass.name()));
        targetArray.visitEnd();
        mixinAnnotation.visitEnd();
    }

    @LuaWrapped
    public MixinEventType inject(String eventName, LuaTable annotations, @OptionalArg List<MixinLib.LuaLocal> locals) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");

        LuaAnnotation luaAnnotation = new LuaAnnotation(
                script.getExecutor().getState(),
                annotations,
                visitedClass,
                EClass.fromJava(Inject.class)
        );

        return writeInject(eventName, luaAnnotation, locals);
    }

//    @LuaWrapped
    public MixinEventType redirect(String eventName, LuaTable annotations, @OptionalArg List<MixinLib.LuaLocal> locals, @OptionalArg Boolean instanceOf) throws LuaError, InvalidMixinException, InvalidArgumentException {
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");
        // TODO: This doesn't actually work.
        // TODO: instanceOf mode

        LuaAnnotation luaAnnotation = new LuaAnnotation(
                script.getExecutor().getState(),
                annotations,
                visitedClass,
                EClass.fromJava(Redirect.class)
        );

        return writeInject(eventName, luaAnnotation, locals);
    }

    // There's some disadvantages to this system. All shadowed values are made public, and forced to be modifiable.
    // Generally that's what's desired so can ya fault me for doing it this way?
    // TODO: Establish a way to access shadowed values. Or how about a custom injector that does what @Local does for fields?
//    @LuaWrapped
    public void shadow(String target) {
        if (visitedClass.containsField(target)) {
            VisitedField visitedField = visitedClass.getField(target);
            FieldVisitor fieldVisitor = c.visitField(
                    visitedField.access() | ACC_PUBLIC,
                    visitedField.descriptor(),
                    visitedField.name(),
                    visitedField.signature(),
                    null
            );
            //noinspection DuplicatedCode
            attachAnnotation(fieldVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedField.access() & ACC_FINAL) != 0) {
                attachAnnotation(fieldVisitor, Final.class).visitEnd();
            }
            if ((visitedField.access() & ACC_SYNTHETIC) != 0) {
                attachAnnotation(fieldVisitor, Dynamic.class).visitEnd();
            }
            fieldVisitor.visitEnd();
        } else if (visitedClass.containsMethod(target)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(target);
            MethodVisitor methodVisitor = c.visitMethod(
                    visitedMethod.access() | ACC_PUBLIC | ACC_ABSTRACT,
                    visitedMethod.descriptor(),
                    visitedMethod.name(),
                    visitedMethod.signature(),
                    null
            );
            //noinspection DuplicatedCode
            attachAnnotation(methodVisitor, Shadow.class).visitEnd();
            // Automagically add other annotations, may or may not be needed.
            if ((visitedMethod.access() & ACC_FINAL) != 0) {
                attachAnnotation(methodVisitor, Final.class).visitEnd();
            }
            if ((visitedMethod.access() & ACC_SYNTHETIC) != 0) {
                attachAnnotation(methodVisitor, Dynamic.class).visitEnd();
            }
            methodVisitor.visitEnd();
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
        writeAccessor(true, annotations);
    }

    @LuaWrapped
    public void getAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        writeAccessor(false, annotations);
    }

    @LuaWrapped
    public void invoker(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String methodName = getTargetValue(annotations);
        if (visitedClass.containsMethod(methodName)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(methodName);
            String mappedName = visitedMethod.mappedName(script.getExecutor().getState());
            mappedName = "invoke" +
                    mappedName.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    mappedName.substring(1);// Rest of name

            LuaAnnotation luaAnnotation = new LuaAnnotation(
                    script.getExecutor().getState(),
                    annotations,
                    visitedClass,
                    EClass.fromJava(Invoker.class)
            );

            this.writeMethod(
                    visitedMethod,
                    visitedMethod.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    mappedName,
                    List.of(Type.getArgumentTypes(visitedMethod.descriptor())),
                    Type.getReturnType(visitedMethod.descriptor()),
                    visitedMethod.needsInstance() ? createInvokerWriteFactory() : null,
                    luaAnnotation
            );
        }
    }

    private MethodWriteFactory createInvokerWriteFactory() {
        return (visitor, desc, offset) -> {
            AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(AssertionError.class), "()V").run();
            visitor.visitInsn(ATHROW);
            visitor.visitMaxs(0,0);
        };
    }

    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String fieldName = getTargetValue(annotations);
        if (visitedClass.containsField(fieldName)) {
            VisitedField visitedField = visitedClass.getField(fieldName);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            String mappedName = visitedField.mappedName(script.getExecutor().getState());
            mappedName = (isSetter ? "set" : "get") + // set or get
                    mappedName.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    mappedName.substring(1);

            LuaAnnotation luaAnnotation = new LuaAnnotation(
                    script.getExecutor().getState(),
                    annotations,
                    visitedClass,
                    EClass.fromJava(Accessor.class)
            );

            this.writeMethod(
                    visitedField,
                    visitedField.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT),
                    mappedName, // Rest of name
                    isSetter ? List.of(visitedFieldType) : List.of(),
                    isSetter ? Type.VOID_TYPE : visitedFieldType,
                    visitedField.needsInstance() ? createAccessorWriteFactory() : null,
                    luaAnnotation
            );
        }
    }

    private MethodWriteFactory createAccessorWriteFactory() {
        return (visitor, desc, offset) -> {
            AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(UnsupportedOperationException.class), "()V").run();
            visitor.visitInsn(ATHROW);
            visitor.visitMaxs(0,0);
        };
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

    private MixinEventType writeInject(String eventName, LuaAnnotation annotation, @Nullable List<MixinLib.LuaLocal> locals) throws LuaError, InvalidMixinException, InvalidArgumentException {
        String descriptor = annotation.findElement("method", String.class);
        if (visitedClass.containsMethod(descriptor)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(descriptor);
            List<Type> params = visitedMethod.getParams();
            params.add(Type.getType(CallbackInfo.class));

            int localOffset = -1;
            if (locals != null) {
                localOffset = params.size();
                locals.forEach((local) -> params.add(Type.getType(local.type())));
            }

            List<Type> paramTypes = new ArrayList<>(params);

            if ((visitedMethod.access() & ACC_STATIC) == 0) {
                paramTypes.add(0, visitedClass.getType());
            }

            this.writeMethod(
                    visitedMethod,
                    ACC_PRIVATE,
                    script.getID() + "$" +
                            visitedMethod.name()
                            .replace("<", "")
                            .replace(">", "") +
                            methodIndex++,
                    params,
                    Type.VOID_TYPE,
                    createInjectWriteFactory(eventName, visitedMethod, paramTypes, localOffset, locals),
                    annotation
            );

            List<String> paramNames = new ArrayList<>();
            paramTypes.forEach((type) -> paramNames.add(AsmUtil.getWrappedTypeName(type)));
            return new MixinEventType(script.getID() + ":" + eventName, paramNames);

        } else {
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        }
    }

    private MethodWriteFactory createInjectWriteFactory(String eventName, VisitedMethod visitedMethod, List<Type> paramTypes, int localOffset, @Nullable List<MixinLib.LuaLocal> locals) {
        return (methodVisitor, desc, thisVarOffset) -> {
            // descriptor +1 for CallbackInfo
            if (localOffset > 0 && locals != null) {
                for (int i = 0; i < locals.size(); i++) {
                    AnnotationVisitor paramAnnoVisitor = attachParameterAnnotation(methodVisitor, i+localOffset, Local.class);
                    locals.get(i).visit(paramAnnoVisitor);
                    paramAnnoVisitor.visitEnd();
                }
            }

            int varPrefix = (Type.getArgumentsAndReturnSizes(visitedMethod.descriptor()) >> 2)+(locals == null ? 0 : locals.size())+1;
            AsmUtil.createArray(methodVisitor, varPrefix, paramTypes, Object.class, (visitor, index, arg) -> {
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

    private void writeMethod(
            VisitedElement visitedValue,
            int accessOverride,
            String name,
            List<Type> params,
            Type returnType,
            MethodWriteFactory writeFactory,
            LuaAnnotation annotation
    ) throws LuaError, InvalidArgumentException, InvalidMixinException {
        String desc = Type.getMethodDescriptor(returnType, params.toArray(Type[]::new));
        MethodVisitor methodVisitor = c.visitMethod(
                accessOverride | (visitedValue.access() & ACC_STATIC),
                name,
                desc,
                visitedValue instanceof VisitedMethod ? visitedValue.signature() : null,
                // I doubt this is necessary, but just in case.
                visitedValue instanceof VisitedMethod ? ((VisitedMethod) visitedValue).exceptions() : null
        );
        int thisVarOffset = (visitedValue.access() & ACC_STATIC) != 0 ? 0 : 1;

        AnnotationVisitor annotationVisitor = attachAnnotation(methodVisitor, annotation.type().raw());
        annotation.apply(annotationVisitor);// Apply annotations to this method
        annotationVisitor.visitEnd();

        if (writeFactory != null) {
            methodVisitor.visitCode();
            writeFactory.write(methodVisitor, desc, thisVarOffset);
            methodVisitor.visitEnd();
        }
    }

    // I'm a hater of how ClassVisitor, MethodVisitor, FieldVisitor, etc. aren't all under a common interface.
    private static AnnotationVisitor attachAnnotation(MethodVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    private static AnnotationVisitor attachParameterAnnotation(
            MethodVisitor visitor,
            int index,
            @SuppressWarnings("SameParameterValue") Class<?> annotation
    ) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitParameterAnnotation(index,
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    private static AnnotationVisitor attachAnnotation(FieldVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    @LuaWrapped
    public MixinClassInfo build() {
        c.visitEnd();
        byte[] classBytes = c.toByteArray();
        AsmUtil.dumpClass(className, classBytes);

        // give the class back to the user for later use in the case of an interface.
        MixinClassInfo info = new MixinClassInfo(className, classBytes, this.duck);

        Registry<MixinClassInfo> registry = (targetEnvironment == null) ? MIXINS : switch (targetEnvironment) {
            case SERVER -> SERVER;
            case CLIENT -> CLIENT;
        };
        registry.register(info);
        return info;
    }

    @FunctionalInterface
    private interface MethodWriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, int thisVarOffset) throws InvalidArgumentException, LuaError, InvalidMixinException;
    }

}


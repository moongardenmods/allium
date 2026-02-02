package dev.hugeblank.allium.loader.lib.builder;

import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import dev.hugeblank.allium.util.Pair;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder extends AbstractClassBuilder {
    public static final Map<String, Pair<List<FieldDefinition>, List<FieldDefinition>>> FIELD_REFERENCES = new HashMap<>();
    private static final EClass<?> VOID = EClass.fromJava(Void.class);

    private final LuaState state;
    private final EClass<?> parentClass;
    private final List<EMethod> methods = new ArrayList<>();
    private final Map<String, MethodReference> methodReferences = new HashMap<>();
    private final Queue<ConstructorReference> ctorReferences = new LinkedList<>();
    private final List<FieldReference> instanceFields = new ArrayList<>();
    private final List<FieldReference> classFields = new ArrayList<>();
    private final FieldBuilder fields;

    private boolean hasConstructor = false;

    @LuaWrapped
    public ClassBuilder(EClass<?> superClass, List<EClass<?>> interfaces, Map<String, Boolean> access, LuaState state) {
        super(
                AsmUtil.getUniqueClassName(),
                superClass.name().replace('.', '/'),
                interfaces.stream()
                        .map(x -> x.name().replace('.', '/'))
                        .toArray(String[]::new),
                ACC_PUBLIC |
                        (access.getOrDefault("interface", false) ? ACC_INTERFACE | ACC_ABSTRACT : 0) |
                        (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0),
                null
        );
        this.state = state;
        this.parentClass = superClass;
        this.fields = new FieldBuilder(className, c);

        final List<EMethod> methods = new ArrayList<>(this.parentClass.methods());
        EClass<?> eClass = this.parentClass;
        while (eClass != null) {
            methods.addAll(eClass.declaredMethods());
            eClass = eClass.superclass();
        }

        for (var inrf : interfaces) {
            methods.addAll(inrf.methods());
        }

        this.methods.addAll(methods.stream().distinct().filter((m) -> m.isPublic() || m.isProtected()).toList());
    }

    @LuaWrapped
    public void put(String key, LuaValue value) {
        if (methodReferences.containsKey(key)) {
            if (value instanceof LuaFunction function) {
                writeMethod(methodReferences.remove(key), function);
            } else {
                throw new IllegalStateException("Expected function for '" + key + "', got " + value.typeName());
            }
        } else if (key.equals("constructor") && !ctorReferences.isEmpty()) {
            if (value instanceof LuaFunction function) {
                writeMethod(ctorReferences.remove().toMethodReference(), function);
                hasConstructor = true;
            } else {
                throw new IllegalStateException("Expected function for constructor, got " + value.typeName());
            }
        } else {
            throw new IllegalStateException("No such method or constructor '" + key + "' exists for application on class");
        }
    }

    public void field(@LuaStateArg LuaState state, String fieldName, EClass<?> type, Map<String, Boolean> access, LuaFunction definition) {
        int intAccess = handleMethodAccess(access);
        FieldDefinition fieldDefinition = (userdata) -> {
            if (definition == null) return null;
            synchronized (state) {
                LuaValue value = Dispatch.call(state, definition, userdata).arg(1);
                return TypeCoercions.toJava(state, value, type);
            }
        };

        if (access.getOrDefault("static", false)) {
            classFields.add(new FieldReference(fieldName, type, fieldDefinition, intAccess));
        } else {
            instanceFields.add(new FieldReference(fieldName, type, fieldDefinition, intAccess));
        }
        c.visitField(intAccess, fieldName, Type.getDescriptor(type.raw()), null, null);
    }

    @LuaWrapped
    public void field(@LuaStateArg LuaState state, String fieldName, EClass<?> type, Map<String, Boolean> access, LuaValue value) throws InvalidArgumentException, LuaError {
        if (access.getOrDefault("final", false) && value == null) {
            throw new IllegalStateException("Final field '" + fieldName + "' must have definition");
        }
        if (value instanceof LuaFunction func) {
            field(state, fieldName, type, access, func);
            return;
        }

        int intAccess = handleMethodAccess(access);
        c.visitField(intAccess, fieldName, Type.getDescriptor(type.raw()), null, value == Constants.NIL ? null : TypeCoercions.toJava(state, value, type));
    }

    @LuaWrapped
    public void constructor(List<EClass<?>> parameters, @OptionalArg Map<String, Boolean> access) {
        constructor(parameters, access, false);
    }

    @LuaWrapped
    public void constructor(List<EClass<?>> parameters, @OptionalArg Map<String, Boolean> access, boolean definesFields) {
        if ((this.access & ACC_INTERFACE) == ACC_INTERFACE) {
            throw new IllegalStateException("Interfaces can not contain a constructor");
        }
        var ctors = this.parentClass.constructors().stream().filter((m) -> !m.isPrivate()).toList();
        for (EConstructor<?> ctor : ctors) {
            List<EParameter> ctorParams = ctor.parameters();

            if (ctorParams.size() == parameters.size()) {
                boolean match = true;
                for (int i = 0; i < parameters.size(); i++) {
                    if (!ctorParams.get(i).parameterType().upperBound().raw().equals(parameters.get(i).raw())) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    ctorReferences.add(new ConstructorReference(
                        ctor,
                        ctorParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                        ctor.modifiers(),
                        definesFields
                    ));
                    return;
                }
            }
        }
        if (access == null) throw new IllegalStateException("Expected access modifiers for constructor not matching super");
        ctorReferences.add(new ConstructorReference(
            null,
            parameters.stream().map((ec) -> new WrappedType(ec, ec)).toArray(WrappedType[]::new),
            handleMethodAccess(access),
            definesFields
        ));
    }

    @LuaWrapped
    public void override(String methodName, List<EClass<?>> parameters) throws LuaError {
        var methods = new ArrayList<EMethod>();
        PropertyResolver.collectMethods(this.methods.stream().filter((m) -> !m.isPrivate()).toList(), methodName, methods::add);

        for (EMethod method : methods) {
            List<EParameter> methParams = method.parameters();

            if (methParams.size() == parameters.size()) {
                boolean match = true;
                for (int i = 0; i < parameters.size(); i++) {
                    if (!methParams.get(i).parameterType().upperBound().raw().equals(parameters.get(i).raw())) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    methodReferences.put(method.name(), new MethodReference(method.name(),
                            methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                            new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
                            method.modifiers() & ~ACC_ABSTRACT));
                    return;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find method " + methodName + " in parent class " + parentClass.name() + "!");
    }

    @LuaWrapped
    public void method(String methodName, List<EClass<?>> parameters, @Nullable EClass<?> returnClass, Map<String, Boolean> access) throws LuaError {
        int accessInt = handleMethodAccess(access);
        if ((accessInt & ACC_ABSTRACT) == 0) {
            methodReferences.put(methodName, new MethodReference(methodName,
                    parameters.stream().map(x -> new WrappedType(x, x)).toArray(WrappedType[]::new),
                    returnClass == null ? null : new WrappedType(returnClass, returnClass),
                    accessInt));
        }
    }

    private int handleMethodAccess(Map<String, Boolean> access) {
        final int out;
        if (access.getOrDefault("private", false)) {
            out = ACC_PRIVATE;
        } else if (access.getOrDefault("protected", false)) {
            out = ACC_PROTECTED;
        } else {
            out = ACC_PUBLIC;
        }
        return out |
            (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0) |
            (access.getOrDefault("static", false) ? ACC_STATIC : 0) |
            (access.getOrDefault("final", false) ? ACC_FINAL : 0);
    }

    private void writeMethod(MethodReference reference, @Nullable LuaFunction func) {
        Type[] paramsType = Arrays.stream(reference.params).map(x -> x.raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        Type returnType = reference.returns == null ? Type.VOID_TYPE : Type.getType(reference.returns.raw.raw());
        boolean isStatic = (reference.access & ACC_STATIC) != 0;

        String desc = Type.getMethodDescriptor(returnType, paramsType);
        MethodVisitor m = c.visitMethod(reference.access, reference.name, desc, null, null);
        int arrayPos = Type.getArgumentsAndReturnSizes(desc) >> 2;
        int thisVarOffset = isStatic ? 0 : 1;

        if (func != null) {
            m.visitCode();

            if (isStatic) arrayPos -= 1;

            m.visitLdcInsn(reference.params.length + thisVarOffset);
            m.visitTypeInsn(ANEWARRAY, Owners.LUA_VALUE);
            m.visitVarInsn(ASTORE, arrayPos);

            if (!isStatic) {
                String eClass = fields.storeAndGetComplex(m, EClass::fromJava, EClass.class, className); // thisEClass
                m.visitMethodInsn(INVOKESTATIC, Owners.PRIVATE_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdataFactory;", false); // privateUDF
                m.visitVarInsn(ALOAD, 0); // privateUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdata;", false); // privateLuaValue
                m.visitVarInsn(ASTORE, arrayPos+1); //
                fields.get(m, eClass, EClass.class); // thisEClass
                m.visitMethodInsn(INVOKESTATIC, Owners.SUPER_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdataFactory;", false); // superUDF
                m.visitVarInsn(ALOAD, 0); // superUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.SUPER_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;", false); // superLuaValue
                m.visitTypeInsn(CHECKCAST, Owners.SUPER_USERDATA);
                m.visitVarInsn(ALOAD, arrayPos+1); // superLuaValue, privateLuaValue
                m.visitTypeInsn(CHECKCAST, Owners.PRIVATE_USERDATA);
                m.visitInsn(SWAP); // privateLuaValue, superLuaValue
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA, "applySuperInstance", "(Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;)V", false); //

                m.visitVarInsn(ALOAD, arrayPos); // array
                m.visitLdcInsn(0); // array, 0
                m.visitVarInsn(ALOAD, arrayPos+1); // array, 0, luaValue
                m.visitInsn(AASTORE); //
            }

            int argIndex = thisVarOffset;
            var args = Type.getArgumentTypes(desc);
            for (int i = 0; i < args.length; i++) {
                m.visitVarInsn(ALOAD, arrayPos); // param
                m.visitLdcInsn(i + thisVarOffset); // param, index
                m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex); // param, index, ???

                if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                    AsmUtil.wrapPrimitive(m, args[i]);
                }

                fields.storeAndGet(m, reference.params[i].real.wrapPrimitive(), EClass.class);
                m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
                m.visitInsn(AASTORE);

                argIndex += args[i].getSize();
            }

            var isVoid = reference.returns == null || returnType.getSort() == Type.VOID;

            fields.storeAndGet(m, state, LuaState.class); // state
            if (!isVoid) m.visitInsn(DUP); // state, state?
            fields.storeAndGet(m, func, LuaFunction.class); // state, state?, function
            m.visitVarInsn(ALOAD, arrayPos); // state, state, function, luavalue[]
            m.visitMethodInsn(INVOKESTATIC, Owners.VALUE_FACTORY, "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
            m.visitMethodInsn(INVOKESTATIC, Owners.DISPATCH, "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs

            if (!isVoid) {
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.VARARGS, "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state? luavalue
                fields.storeAndGet(m, reference.returns.real.wrapPrimitive(), EClass.class); // state?, luavalue, eclass
                m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.returns.real.wrapPrimitive().raw())); // object(return type)

                if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                    AsmUtil.unwrapPrimitive(m, returnType); // primitive
                }
            }

            m.visitInsn(returnType.getOpcode(IRETURN));

            m.visitMaxs(0, 0);
        }

        m.visitEnd();
    }

    @LuaWrapped
    public LuaValue build() {
        Pair<List<FieldDefinition>, List<FieldDefinition>> pair = new Pair<>(new ArrayList<>(), new ArrayList<>());
        FIELD_REFERENCES.put(className, pair);
        if (!classFields.isEmpty()) {
            MethodVisitor clinit = c.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.visitCode();

            applyFieldDefs(classFields, pair, clinit, true);

            clinit.visitInsn(RETURN);
            clinit.visitMaxs(0, 0);
            clinit.visitEnd();
        }

        if (!parentClass.constructors().isEmpty()) {
            FIELD_REFERENCES.computeIfAbsent(className, (_) -> new Pair<>(new ArrayList<>(), new ArrayList<>()));

            while (!ctorReferences.isEmpty()) {
                ConstructorReference reference = ctorReferences.remove();

                StringBuilder builder = new StringBuilder("(");
                builder.append(reference.params.length).append(" arguments) ");

                for (int i = 0; i < reference.params.length; i++) {
                    builder.append(reference.params[i].raw);
                    if (i < reference.params.length-1) builder.append(", ");
                }

                if (reference.ctor == null) throw new IllegalStateException("Missing function for constructor with no match on super class: " + builder);

                EConstructor<?> superCtor = reference.ctor;

                String desc = Type.getConstructorDescriptor(superCtor.raw());
                MethodVisitor m = c.visitMethod(superCtor.modifiers(), "<init>", desc, null, null);
                m.visitCode();
                Type[] args = Type.getArgumentTypes(desc);

                m.visitVarInsn(ALOAD, 0);

                int argIndex = 1;

                for (Type arg : args) {
                    m.visitVarInsn(arg.getOpcode(ILOAD), argIndex);

                    argIndex += arg.getSize();
                }

                m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(parentClass.raw()), "<init>", desc, false);

                if (reference.definesFields()) applyFieldDefs(instanceFields, pair, m, false);

                m.visitInsn(RETURN);

                m.visitMaxs(0, 0);
                m.visitEnd();

                hasConstructor = true;
            }

            if (!hasConstructor) {
                throw new IllegalStateException("Missing constructor for class whose super class defines one");
            }
        }

        if (!methodReferences.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            Iterator<MethodReference> missingReferences = methodReferences.values().iterator();
            while (missingReferences.hasNext() && missingReferences.next() instanceof MethodReference reference) {
                builder.append(reference.name());
                if (missingReferences.hasNext()) builder.append(", ");
            }
            throw new IllegalStateException("Missing functions for method(s): " + builder);
        }

        byte[] classBytes = c.toByteArray();

        Class<?> klass = AsmUtil.defineClass(className, classBytes);

        fields.apply(klass);

        return StaticBinder.bindClass(EClass.fromJava(klass));
    }

    private void applyFieldDefs(List<FieldReference> fieldReferences, Pair<List<FieldDefinition>, List<FieldDefinition>> pair, MethodVisitor m, boolean isStatic) {
        List<FieldDefinition> definitions = isStatic ? pair.left() : pair.right();
        for (FieldReference fieldReference : fieldReferences) {
            Class<?> rawType = fieldReference.type.raw();
            int pos = definitions.size();
            definitions.add(fieldReference.definition());
            m.visitFieldInsn(GETSTATIC, Owners.CLASS_BUILDER, "FIELD_REFERENCES", Type.getDescriptor(Map.class)); // <- map
            m.visitLdcInsn(className); // map <- className
            m.visitMethodInsn(INVOKEINTERFACE, Owners.MAP, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true); // -> map, className <- pair
            m.visitTypeInsn(CHECKCAST, Owners.PAIR); // cast
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.PAIR, isStatic ? "left" : "right", "()Ljava/lang/Object;", false); // -> pair <- list
            m.visitTypeInsn(CHECKCAST, Owners.LIST); // cast
            m.visitLdcInsn(pos); // list <- pos
            m.visitMethodInsn(INVOKEINTERFACE, Owners.LIST, "get", "(I)Ljava/lang/Object;", true); // <- definition -> list, pos
            m.visitTypeInsn(CHECKCAST, "dev/hugeblank/allium/loader/lib/builder/ClassBuilder$FieldDefinition"); // cast
            m.visitLdcInsn(Type.getType("L" + className + ";")); // definition <- class
            m.visitMethodInsn(INVOKESTATIC, Owners.ECLASS, "fromJava", "(Ljava/lang/Class;)Lme/basiqueevangelist/enhancedreflection/api/EClass;", true); // definition <-> classEClass

            if (isStatic) {
                m.visitMethodInsn(INVOKESTATIC, Owners.STATIC_BINDER, "bindClass", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/ClassUserdata;", false); // definition <-> userdata
            } else {
                m.visitMethodInsn(INVOKESTATIC, Owners.INSTANCE_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/InstanceUserdataFactory;", false); // definition <-> thisUserdataFactory
                m.visitVarInsn(ALOAD, 0); // definition, thisUserdataFactory <- this
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.INSTANCE_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/InstanceUserdata;", false); // definition -> thisUserdataFactory, this <- userdata
            }

            m.visitMethodInsn(INVOKEINTERFACE, "dev/hugeblank/allium/loader/lib/builder/ClassBuilder$FieldDefinition", "apply", "(Lorg/squiddev/cobalt/LuaUserdata;)Ljava/lang/Object;", true); //  -> definition, userdata <- fieldObject
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(fieldReference.type().wrapPrimitive().raw())); // cast
            if (rawType.isPrimitive()) {
                AsmUtil.unwrapPrimitive(m, Type.getType(rawType)); // <-> realFieldValue
            }

            if (isStatic) {
                m.visitFieldInsn(PUTSTATIC, className, fieldReference.name(), Type.getDescriptor(rawType)); // -> realFieldValue
            } else {
                m.visitVarInsn(ALOAD, 0); // realFieldValue <- this
                m.visitInsn(SWAP); // this, realFieldValue
                m.visitFieldInsn(PUTFIELD, className, fieldReference.name(), Type.getDescriptor(fieldReference.type().raw())); // -> this, realFieldValue
            }
        }
    }

    public String getName() {
        return this.className;
    }

    private record WrappedType(EClass<?> raw, EClass<?> real) {
        public static WrappedType fromParameter(EParameter param) {
            return new WrappedType(param.rawParameterType(), param.parameterType().lowerBound());
        }
    }

    private record ConstructorReference(EConstructor<?> ctor, WrappedType[] params, int access, boolean definesFields) {
        public MethodReference toMethodReference() {
            return new MethodReference("<init>", params, new WrappedType(VOID, VOID), access);
        }
    }

    public interface FieldDefinition {
        Object apply(LuaUserdata luaUserdata) throws LuaError, UnwindThrowable, InvalidArgumentException;
    }

    private record FieldReference(String name, EClass<?> type, FieldDefinition definition, int access) {}

    private record MethodReference(String name, WrappedType[] params, WrappedType returns, int access) {}
}

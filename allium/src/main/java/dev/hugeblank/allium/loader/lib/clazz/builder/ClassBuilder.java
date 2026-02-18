package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.loader.lib.clazz.definition.ConstructorDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.ExecutableDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.MethodDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.WrappedType;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.LuaStateArg;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.api.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder extends AbstractClassBuilder {
    private static final Map<Object, FieldHolder> INSTANCE_FIELD_HOOKS = new ConcurrentHashMap<>();
    private static final Map<String, FieldHolder> CLASS_FIELD_HOOKS = new ConcurrentHashMap<>();
    public static final EClass<?> VOID = EClass.fromJava(void.class);

    private final LuaState state;
    final EClass<?> parentClass;
    final List<EMethod> methods = new ArrayList<>();

    private final List<FieldReference> instanceFields = new ArrayList<>();
    private final List<FieldReference> classFields = new ArrayList<>();
    private final InternalFieldBuilder fields;

    final Map<String, List<MethodReference>> methodReferences = new HashMap<>();
    final Map<String, MethodDefinition> methodDefinitions = new HashMap<>();
    final List<ConstructorDefinition> ctorDefinitions = new ArrayList<>();

    boolean definesValidConstructor = false;

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
        this.fields = new InternalFieldBuilder(className, c);

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
        if (value instanceof LuaFunction function) {
            if (methodDefinitions.containsKey(key)) {
                MethodDefinition def = methodDefinitions.remove(key);
                checkAppendMethodReference(
                    def, function,
                    "Method '" + def.name() + "' cannot define two methods with the same parameters."
                );
            } else if (key.equals("clinit")) {
                methodReferences.compute("<clinit>", (_, v) -> {
                    MethodReference ref = new MethodReference(new ExecutableDefinition(
                        "<clinit>",
                        new WrappedType[]{},
                        new WrappedType(VOID, VOID),
                        ACC_STATIC,
                        true
                    ), function);
                    if (v == null) return new ArrayList<>(List.of(ref));
                    v.add(ref);
                    return v;
                });
            } else {
                throw new IllegalStateException("No such method or constructor '" + key + "' exists for application on class");
            }
        } else {
            throw new IllegalStateException("Expected function for '" + key + "', got " + value.typeName());
        }
    }

    private void checkAppendMethodReference(
        ExecutableDefinition def, @Nullable LuaFunction function, String identicalMessage
    ) {
        methodReferences.compute(def.name(), (_, v) -> {
            if (v == null) return new ArrayList<>(List.of(new MethodReference(def, function)));
            for (MethodReference reference : v) {
                if (reference.definition().params().length == def.params().length) {
                    boolean match = true;
                    for (int i = 0; i < def.params().length; i++) {
                        if (!def.params()[i].equals(reference.definition().params()[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        throw new IllegalStateException(
                            identicalMessage + " Identical paramaters are:\n" + buildParameters(def)
                        );
                    }
                }
            }
            v.add(new MethodReference(def, function));
            return v;
        });
    }

    private static String buildParameters(ExecutableDefinition def) {
        StringBuilder builder = new StringBuilder("(");
        builder.append(def.params().length).append(" arguments) ");

        for (int i = 0; i < def.params().length; i++) {
            builder.append(def.params()[i].raw());
            if (i < def.params().length-1) builder.append(", ");
        }
        return builder.toString();
    }

    @LuaWrapped
    public void field(@LuaStateArg LuaState state, String fieldName, EClass<?> type, Map<String, Boolean> access, @OptionalArg LuaValue value) throws InvalidArgumentException, LuaError {
        if (fieldName.startsWith("allium$")) {
            throw new IllegalStateException("Fields that start with the allium$ ID prefix are not permitted in generated classes.");
        }
        int intAccess = handleMethodAccess(access);
        FieldReference ref = new FieldReference(fieldName, type, intAccess);
        c.visitField(intAccess, fieldName, Type.getDescriptor(type.raw()), null, value == null || value == Constants.NIL ? null : TypeCoercions.toJava(state, value, type));
        if (access.getOrDefault("static", false)) {
            classFields.add(ref);
        } else {
            instanceFields.add(ref);
        }
    }

    @LuaWrapped
    public ConstructorBuilder constructor() {
        return new ConstructorBuilder(this);
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
                    methodDefinitions.put(method.name(), new MethodDefinition(method.name(),
                        methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                        new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
                        method.modifiers() & ~ACC_ABSTRACT
                    ));
                    return;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find method " + methodName + " in parent class " + parentClass.name() + "!");
    }

    @LuaWrapped
    public void method(String methodName, List<EClass<?>> parameters, @Nullable EClass<?> returnClass, Map<String, Boolean> access) throws LuaError {
        MethodDefinition definition = new MethodDefinition(methodName,
            parameters.stream().map(x -> new WrappedType(x, x)).toArray(WrappedType[]::new),
            returnClass == null ? new WrappedType(VOID, VOID) : new WrappedType(returnClass, returnClass),
            handleMethodAccess(access)
        );
        if (access.getOrDefault("abstract", false)) {
            checkAppendMethodReference(
                definition, null,
                "Method '" + definition.name() + "' cannot define two methods with the same parameters."
            );
            return;
        }
        methodDefinitions.put(methodName, definition);
    }

    public static int handleMethodAccess(Map<String, Boolean> access) {
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

    private void writeMethod(MethodReference reference) {
        ExecutableDefinition definition = reference.definition();
        Type[] paramsType = Arrays.stream(definition.params()).map(WrappedType::raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        Type returnType = definition.returns() == null ? Type.VOID_TYPE : Type.getType(definition.returns().raw().raw());
        boolean isStatic = (definition.access() & ACC_STATIC) != 0;
        boolean isAbstract = (definition.access() & ACC_ABSTRACT) != 0;
        boolean isVoid = definition.returns() == null || returnType.getSort() == Type.VOID;

        String desc = Type.getMethodDescriptor(returnType, paramsType);
        MethodVisitor m = c.visitMethod(definition.access(), definition.name(), desc, null, null);

        if (reference.function() != null || definition instanceof ConstructorDefinition) {
            int arrayPos = Type.getArgumentsAndReturnSizes(desc) >> 2;
            int paramOffset = isStatic && !definition.name().equals("<clinit>") ? 0 : 1;

            m.visitCode();

            if (isStatic) arrayPos -= 1;

            // net 0 effect on stack
//            if (definition instanceof ConstructorDefinition constructorDefinition) {
//                arrayPos = constructorDefinition.applyRemap(state);
//                addToFieldHooks(m);
//            }

            m.visitLdcInsn(definition.params().length + paramOffset);
            m.visitTypeInsn(ANEWARRAY, Owners.LUA_VALUE);
            m.visitVarInsn(ASTORE, arrayPos);

            if (!isStatic) {
                String eClass = fields.storeAndGetComplex(m, EClass::fromJava, EClass.class, className); // thisEClass
                m.visitMethodInsn(INVOKESTATIC, Owners.SUPER_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdataFactory;", false); // superUDF
                m.visitVarInsn(ALOAD, 0); // superUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.SUPER_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;", false); // superLuaValue
                m.visitTypeInsn(CHECKCAST, Owners.SUPER_USERDATA);
                fields.get(m, eClass); // superLuaValue, thisEClass
                m.visitMethodInsn(INVOKESTATIC, Owners.PRIVATE_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdataFactory;", false); // superLuaValue, privateUDF
                m.visitVarInsn(ALOAD, 0); // superLuaValue, privateUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdata;", false); // superLuaValue, privateLuaValue
                m.visitTypeInsn(CHECKCAST, Owners.PRIVATE_USERDATA);
                m.visitInsn(DUP); // superLuaValue, privateLuaValue, privateLuaValue
                m.visitVarInsn(ALOAD, arrayPos); // superLuaValue, privateLuaValue, privateLuaValue, array
                m.visitInsn(SWAP); // superLuaValue, privateLuaValue, array, privateLuaValue
                m.visitLdcInsn(0); // superLuaValue, privateLuaValue, array, privateLuaValue, 0
                m.visitInsn(SWAP); // superLuaValue, privateLuaValue, array, 0, privateLuaValue
                m.visitInsn(AASTORE); // superLuaValue, privateLuaValue
                m.visitInsn(SWAP); // privateLuaValue, superLuaValue
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA, "applySuperInstance", "(Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;)V", false); //
            } else if (definition.name().equals("<clinit>")) {
                m.visitVarInsn(ALOAD, arrayPos); // array
                m.visitLdcInsn(0); // array, 0
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "initClassFieldHolder", "(Ljava/lang/String;)Ldev/hugeblank/allium/loader/lib/builder/ClassBuilder$FieldHolder;", false); // array, 0, holder
                m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false); // array, 0, luaValue
                m.visitInsn(AASTORE); //
            }

            if (reference.function() != null) {
                int argIndex = paramOffset;
                var args = Type.getArgumentTypes(desc);

                for (int i = 0; i < args.length; i++) {
                    m.visitVarInsn(ALOAD, arrayPos); // param
                    m.visitLdcInsn(i + paramOffset); // param, index
                    m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex); // param, index, ???

                    if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                        AsmUtil.wrapPrimitive(m, args[i]);
                    }

                    fields.storeAndGet(m, definition.params()[i].real().wrapPrimitive(), EClass.class);
                    m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
                    m.visitInsn(AASTORE);

                    argIndex += args[i].getSize();
                }

                fields.storeAndGet(m, state, LuaState.class); // state
                if (!isVoid) m.visitInsn(DUP); // state, state?
                fields.storeAndGet(m, reference.function(), LuaFunction.class); // state, state?, function
                m.visitVarInsn(ALOAD, arrayPos); // state, state, function, luavalue[]
                m.visitMethodInsn(INVOKESTATIC, Owners.VALUE_FACTORY, "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
                m.visitMethodInsn(INVOKESTATIC, Owners.DISPATCH, "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs
            }
        }

        // net 0 effect on stack
        if (!isAbstract && reference.definition().definesFields()) {
            applyFieldDefs(isStatic ? classFields : instanceFields, m, isStatic);
            if (reference.definition() instanceof ConstructorDefinition) {
                removeFromFieldHooks(m);
            }
        }

        if (reference.function() != null) {
            if (!isVoid) {
                m.visitMethodInsn(INVOKEVIRTUAL, Owners.VARARGS, "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state, luavalue
                fields.storeAndGet(m, definition.returns().real().wrapPrimitive(), EClass.class); // state, luavalue, eclass
                m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(definition.returns().real().wrapPrimitive().raw())); // object(return type)

                if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                    AsmUtil.unwrapPrimitive(m, returnType); // primitive
                }
            }

        }


        m.visitInsn(returnType.getOpcode(IRETURN));

        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    @LuaWrapped
    public LuaValue build() {
        if (!classFields.isEmpty() && methodReferences.get("<clinit>").isEmpty()) {
            methodReferences.put("<clinit>", List.of(new MethodReference(new ExecutableDefinition(
                "<clinit>",
                new WrappedType[]{},
                new WrappedType(VOID, VOID),
                ACC_STATIC,
                true
            ), null)));
        }

        if (!definesValidConstructor) {
            boolean hasParameterlessCtor = parentClass.constructors().stream()
                .map((c) -> c.parameters().isEmpty())
                .reduce(false, Boolean::logicalOr);
            if (ctorDefinitions.isEmpty() && !parentClass.constructors().isEmpty() && hasParameterlessCtor) {
                throw new IllegalStateException("Parent class has no no-arg constructors. This class must have a constructor that calls `super`.");
            }
        }

        methodReferences.compute(
            "<init>", (_, v) -> {
                List<MethodReference> refs = ctorDefinitions.stream()
                    .map((def) -> new MethodReference(def, null))
                    .toList();
                if (v == null) return refs;
                v.addAll(refs);
                return v;
            }
        );

        if (!methodDefinitions.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            Iterator<MethodDefinition> missingReferences = methodDefinitions.values().iterator();
            while (missingReferences.hasNext() && missingReferences.next() instanceof MethodDefinition reference) {
                builder.append(reference.name());
                if (missingReferences.hasNext()) builder.append(", ");
            }
            throw new IllegalStateException("Missing functions for method(s): " + builder);
        }

        for (Map.Entry<String, List<MethodReference>> entry : methodReferences.entrySet()) {
            List<MethodReference> references = entry.getValue();
            if (entry.getKey().equals("<clinit>") && references.size() > 1) {
                writeMethod(new MethodReference(
                    references.getFirst().definition(),
                    new VarArgFunction() {
                        @Override
                        protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                            LuaValue holder = TypeCoercions.toLuaValue(
                                CLASS_FIELD_HOOKS.compute(
                                    className,
                                    (_, v) -> v == null ? new FieldHolder() : v
                                ),
                                EClass.fromJava(FieldHolder.class)
                            );
                            for (MethodReference ref : references) {
                                if (ref.function() != null) Dispatch.invoke(state, ref.function(), ValueFactory.varargsOf(holder, varargs));
                            }
                            return Constants.NIL;
                        }
                    }
                ));
            } else {
                for (MethodReference reference : references) {
                    writeMethod(reference);
                }
            }
        }

        byte[] classBytes = c.toByteArray();

        Class<?> klass = AsmUtil.defineClass(className, classBytes);

        fields.apply(klass);

        return StaticBinder.bindClass(EClass.fromJava(klass));
    }

    private void applyFieldDefs(List<FieldReference> fieldReferences, MethodVisitor m, boolean isStatic) {
        for (FieldReference reference : fieldReferences) {
            Class<?> rawType = reference.type.raw();

            m.visitLdcInsn(reference.name); // name
            if (isStatic) {
                m.visitLdcInsn(className); // name <- className
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "getField", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false); // -> name, className <- fieldValue
            } else {
                m.visitVarInsn(ALOAD, 0); // name <- this
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "getField", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false); // -> name, this <- fieldValue
            }
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.type().wrapPrimitive().raw())); // cast
            if (rawType.isPrimitive()) {
                AsmUtil.unwrapPrimitive(m, Type.getType(rawType)); // <-> realFieldValue
            }
            if (isStatic) {
                m.visitFieldInsn(PUTSTATIC, className, reference.name(), Type.getDescriptor(rawType)); // -> realFieldValue
            } else {
                m.visitVarInsn(ALOAD, 0); // realFieldValue <- this
                m.visitInsn(SWAP); // this, realFieldValue
                m.visitFieldInsn(PUTFIELD, className, reference.name(), Type.getDescriptor(reference.type().raw())); // -> this, realFieldValue
            }
        }
    }

    public String getName() { return this.className; }

    public static Object getField(String name, Object instance) {
        if (!INSTANCE_FIELD_HOOKS.containsKey(instance)) return null;
        Object out = INSTANCE_FIELD_HOOKS.get(instance).remove(name);
        if (INSTANCE_FIELD_HOOKS.get(instance).isEmpty()) INSTANCE_FIELD_HOOKS.remove(instance);
        return out;
    }

    public static Object getField(String name, String className) {
        if (!CLASS_FIELD_HOOKS.containsKey(className)) return null;
        Object out = CLASS_FIELD_HOOKS.get(className).remove(name);
        if (CLASS_FIELD_HOOKS.get(className).isEmpty()) CLASS_FIELD_HOOKS.remove(className);
        return out;
    }

    public record BuilderContext(
        LuaState state,
        ClassVisitor c,
        String className,
        InternalFieldBuilder fields,
        List<FieldReference> classFields,
        List<FieldReference> instanceFields
    ) {}

    public record FieldReference(String name, EClass<?> type, int access) {}

    record MethodReference(ExecutableDefinition definition, @Nullable LuaFunction function) {}

    public static void addToFieldHooks(MethodVisitor m) {
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "initInstanceFieldHolder", "(Ljava/lang/Object;)V", false);
    }

    public static void removeFromFieldHooks(MethodVisitor m) {
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "removeInstanceFieldHolder", "(Ljava/lang/Object;)V", false);
    }

    public static boolean hasInstanceFieldHooks(Object instance) {
        return INSTANCE_FIELD_HOOKS.containsKey(instance);
    }

    public static void setInstanceFieldHooks(Object instance, String name, Object value) {
        if (INSTANCE_FIELD_HOOKS.containsKey(instance)) {
            INSTANCE_FIELD_HOOKS.get(instance).put(name, value);
        }
    }

    public static FieldHolder initClassFieldHolder(String className) {
        FieldHolder holder = new FieldHolder();
        CLASS_FIELD_HOOKS.putIfAbsent(className, holder);
        return holder;
    }

    public static void initInstanceFieldHolder(Object instance) {
        FieldHolder holder = new FieldHolder();
        INSTANCE_FIELD_HOOKS.putIfAbsent(instance, holder);
    }

    public static void removeInstanceFieldHolder(Object instance) {
        INSTANCE_FIELD_HOOKS.remove(instance);
    }

    public static class FieldHolder {
        private final Map<String, Object> fields = new HashMap<>();

        public FieldHolder() {}

        public void put(String name, Object value) {
            fields.put(name, value);
        }

        public boolean isEmpty() {
            return fields.isEmpty();
        }

        public Object remove(String name) {
            return fields.remove(name);
        }

    }
}

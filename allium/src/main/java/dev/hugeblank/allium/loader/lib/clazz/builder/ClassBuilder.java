package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.loader.lib.clazz.definition.*;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.LuaStateArg;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.api.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.util.asm.AsmUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder extends AbstractClassBuilder {
    private static final Map<Object, FieldHolder> INSTANCE_FIELD_HOOKS = new ConcurrentHashMap<>();
    private static final Map<String, FieldHolder> CLASS_FIELD_HOOKS = new ConcurrentHashMap<>();

    private final LuaState state;
    private final List<FieldReference> instanceFields = new ArrayList<>();
    private final List<FieldReference> classFields = new ArrayList<>();
    private final InternalFieldBuilder fields;
    private final List<EMethod> methods = new ArrayList<>();
    private final EClass<?> parentClass;
    private final Map<String, ExecutableReference> byIndex = new HashMap<>();

    final Map<String, List<ExecutableReference>> definitions = new HashMap<>();

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
    public ClassBuilder field(@LuaStateArg LuaState state, String fieldName, EClass<?> type, Map<String, Boolean> access, @OptionalArg LuaValue value) throws InvalidArgumentException, LuaError {
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
        return this;
    }

    @LuaWrapped
    public ClinitBuilder clinit() {
        return new ClinitBuilder(this, state, this::computeHooks);
    }

    @LuaWrapped
    public ConstructorBuilder constructor() {
        return new ConstructorBuilder(this, this::getConstructors, parentClass);
    }

    @LuaWrapped
    public MethodBuilder method() {
        return new MethodBuilder(this, Collections.unmodifiableList(methods));
    }

    @LuaWrapped
    public ClassBuilder define(LuaTable table) throws LuaError, ClassBuildException {
        LuaValue key = Constants.NIL;
        while (true) {
            Varargs entry = table.next(key);
            key = entry.arg(1);
            if (key == Constants.NIL) break;
            String k = key.checkString();
            ExecutableReference reference = byIndex.get(k);
            if (reference == null) throw new ClassBuildException("No such reference with index '" + k + "'.");
            reference.setFunction(entry.arg(2).checkFunction());
        }
        return this;
    }

    @LuaWrapped
    public LuaValue build() throws ClassBuildException {
        if (!classFields.isEmpty() && definitions.getOrDefault("<clinit>", List.of()).isEmpty()) {
            definitions.put("<clinit>", List.of(new ClinitReference(state, this::computeHooks)));
        }

        if (definitions.getOrDefault("<init>", List.of()).isEmpty()) {
            apply(new DefaultSuperConstructorReference(parentClass));
        }

        if (!definitions.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            boolean invalid = false;
            for (Map.Entry<String, List<ExecutableReference>> entry : definitions.entrySet()) {
                boolean started = false;
                for (ExecutableReference definition : entry.getValue()) {
                    if (!definition.isValid()) {
                        if (!started) {
                            builder.append("  ").append(entry.getKey()).append(" with parameter types:\n");
                            started = true;
                            invalid = true;
                        }
                        builder.append("    ").append(buildParameters(definition));
                    }
                }
            }
            if (invalid) throw new IllegalStateException("Missing functions for method(s): " + builder);
        }

        BuilderContext builderContext = new BuilderContext(state, c, className, fields, classFields, instanceFields);
        for (Map.Entry<String, List<ExecutableReference>> entry : definitions.entrySet()) {
            for (ExecutableReference definition : entry.getValue()) {
                definition.write(builderContext);
            }
        }

        byte[] classBytes = c.toByteArray();

        Class<?> klass = AsmUtil.defineClass(className, classBytes);

        fields.apply(klass);

        return StaticBinder.bindClass(EClass.fromJava(klass));
    }

    public void apply(ExecutableReference definition) throws ClassBuildException {
        definitions.computeIfAbsent(definition.name(), (_) -> new ArrayList<>());
        List<ExecutableReference> defs = definitions.get(definition.name());
        for (ExecutableReference other : defs) {
            final WrappedType[] params = definition.params();
            if (params.length == other.params().length) {
                boolean match = true;
                for (int i = 0; i < params.length; i++) {
                    final WrappedType param = params[i];
                    final WrappedType otherParam = other.params()[i];
                    if (!(param.raw().equals(otherParam.raw()) && param.real().equals(otherParam.real()))) {
                        match = false;
                        break;
                    }
                }
                String type = definition.getTypeName();
                if (match) throw new ClassBuildException(
                    type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1) +
                        " '" + definition.name() + "' cannot define two methods with the same parameters. " +
                        "Identical paramaters are:\n" + buildParameters(definition)
                );
            }
        }

        if (definition.index() != null && byIndex.put(definition.index(), definition) != null)
            throw new ClassBuildException("Index '" + definition.index() + "' is already defined.");

        defs.add(definition);
    }

    private LuaValue computeHooks() {
        return TypeCoercions.toLuaValue(
            CLASS_FIELD_HOOKS.compute(
                className,
                (_, v) -> v == null ? new ClassBuilder.FieldHolder() : v
            ),
            EClass.fromJava(ClassBuilder.FieldHolder.class)
        );
    }

    public static int handleMethodAccess(Map<String, Boolean> access) {
        int out = 0;
        if (access.getOrDefault("private", false)) {
            out = ACC_PRIVATE;
        } else if (access.getOrDefault("protected", false)) {
            out = ACC_PROTECTED;
        } else if (access.getOrDefault("public", false)) {
            out = ACC_PUBLIC;
        }
        return out |
            (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0) |
            (access.getOrDefault("static", false) ? ACC_STATIC : 0) |
            (access.getOrDefault("final", false) ? ACC_FINAL : 0);
    }

    private static String buildParameters(ExecutableReference def) {
        StringBuilder builder = new StringBuilder("(");
        builder.append(def.params().length).append(" arguments) ");

        for (int i = 0; i < def.params().length; i++) {
            builder.append(def.params()[i].raw());
            if (i < def.params().length-1) builder.append(", ");
        }
        return builder.toString();
    }

    private List<ExecutableReference> getConstructors() {
        return Collections.unmodifiableList(
            definitions.computeIfAbsent("<init>", (_) -> new ArrayList<>())
        );
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

    public static boolean hasInstanceFieldHooks(Object instance) {
        return INSTANCE_FIELD_HOOKS.containsKey(instance);
    }

    public static void setInstanceFieldHooks(Object instance, String name, Object value) {
        if (INSTANCE_FIELD_HOOKS.containsKey(instance)) {
            INSTANCE_FIELD_HOOKS.get(instance).put(name, value);
        }
    }

    // Used in bytecode.
    public static Object getField(String name, Object instance) {
        if (!INSTANCE_FIELD_HOOKS.containsKey(instance)) return null;
        Object out = INSTANCE_FIELD_HOOKS.get(instance).remove(name);
        if (INSTANCE_FIELD_HOOKS.get(instance).isEmpty()) INSTANCE_FIELD_HOOKS.remove(instance);
        return out;
    }

    // Used in bytecode.
    public static Object getField(String name, String className) {
        if (!CLASS_FIELD_HOOKS.containsKey(className)) return null;
        Object out = CLASS_FIELD_HOOKS.get(className).remove(name);
        if (CLASS_FIELD_HOOKS.get(className).isEmpty()) CLASS_FIELD_HOOKS.remove(className);
        return out;
    }

    // Used in bytecode.
    public static FieldHolder initClassFieldHolder(String className) {
        FieldHolder holder = new FieldHolder();
        CLASS_FIELD_HOOKS.putIfAbsent(className, holder);
        return holder;
    }

    // Used in bytecode.
    public static void initInstanceFieldHolder(Object instance) {
        FieldHolder holder = new FieldHolder();
        INSTANCE_FIELD_HOOKS.putIfAbsent(instance, holder);
    }

    // Used in bytecode.
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

package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.definition.ConstructorDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.SuperConstructorDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.ThisConstructorDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.WrappedType;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

@LuaWrapped
public class ConstructorBuilder {
    private final ClassBuilder classBuilder;

    private int access = 0;
    private boolean definesFields = false;
    private ConstructorDefinition thisDef = null;
    private EConstructor<?> superDef = null;
    private LuaFunction remapper = null;
    private List<EClass<?>> params = null;


    public ConstructorBuilder(ClassBuilder classBuilder) {
        this.classBuilder = classBuilder;
    }

    public ConstructorBuilder access(Map<String, Boolean> access) {
        this.access = ClassBuilder.handleMethodAccess(access);
        if ((classBuilder.access & ACC_INTERFACE) == ACC_INTERFACE) {
            throw new IllegalStateException("Interfaces can not contain a constructor");
        }
        return this;
    }

    @LuaWrapped(name = "this")
    public ConstructorBuilder usingThis(List<EClass<?>> parameters) {
        ConstructorDefinition def = testDefinitions(parameters);
        if (def == null) throw new IllegalStateException("No such constructor matching parameters.");
        this.thisDef = def;
        return this;
    }

    private ConstructorDefinition testDefinitions(List<EClass<?>> parameters) {
        for (ConstructorDefinition definition : classBuilder.ctorDefinitions) {
            if (testDefinition(parameters, definition)) return definition;
        }
        for (ClassBuilder.MethodReference reference : classBuilder.methodReferences.get("<init>")) {
            if (reference.definition() instanceof ConstructorDefinition ctorDef) {
                if (testDefinition(parameters, ctorDef)) return ctorDef;
            }
        }
        return null;
    }

    private static boolean testDefinition(List<EClass<?>> parameters, ConstructorDefinition definition) {
        if (parameters.size() == definition.params().length) {
            boolean match = true;
            for (int i = 0; i < definition.params().length; i++) {
                if (!definition.params()[i].raw().equals(parameters.get(i))) {
                    match = false;
                    break;
                }
            }
            return match;
        }
        return false;
    }

    @LuaWrapped(name = "super")
    public ConstructorBuilder usingSuper(List<EClass<?>> parameters) {
        List<EConstructor<?>> ctors = classBuilder.parentClass.constructors().stream()
            .filter((m) -> !m.isPrivate())
            .collect(Collectors.toList());

        for (EConstructor<?> ctor : ctors) {
            List<EParameter> ctorParams = ctor.parameters();

            if (ctorParams.size() == parameters.size()) {
                boolean match = true;
                for (int i = 0; i < parameters.size(); i++) {
                    if (!ctorParams.get(i).rawParameterType().raw().equals(parameters.get(i).raw())) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    this.superDef = ctor;
                    return this;
                }
            }
        }
        throw new IllegalStateException("No such super constructor matching parameters.");
    }

    @LuaWrapped
    public ConstructorBuilder parameters(List<EClass<?>> parameters) {
        this.params = parameters;
        return this;
    }

    @LuaWrapped
    public ConstructorBuilder remapper(LuaFunction func) {
        // TODO: When JavaLib gets script, pass it down to here.
//        ScriptRegistry.scriptFromState(state).getLogger().warn("Remapper function defined for constructor that has no custom parameters defined");
        this.remapper = func;
        return this;
    }

    @LuaWrapped
    public ConstructorBuilder definesFields(boolean definesFields) {
        this.definesFields = definesFields;
        return this;
    }

    public ConstructorDefinition build() {
        if (params != null && remapper == null && (thisDef != null || superDef != null)) {
            throw new IllegalStateException(
                "Expected remapper function for constructor with either 'this' or 'super' constructors defined."
            );
        }
        ConstructorDefinition def;
        if (thisDef != null) {
            def = new ThisConstructorDefinition(
                thisDef, remapper, classBuilder.className,
                params == null ?
                    thisDef.params() :
                    params.stream().map((ec) -> new WrappedType(ec, ec)).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else if (superDef != null) {
            def = new SuperConstructorDefinition(
                superDef, remapper,
                (params == null ?
                    superDef.parameters().stream().map(WrappedType::fromParameter) :
                    params.stream().map((ec) -> new WrappedType(ec, ec))
                ).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else if (params != null) {
            // TODO: Can this class accept a constructor that doesn't invoke super when it should?
            def = new ConstructorDefinition(
                remapper,
                params.stream().map((ec) -> new WrappedType(ec, ec)).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else {
            throw new IllegalStateException("Invalid constructor definition.");
        }

        classBuilder.ctorDefinitions.add(def);
        classBuilder.definesValidConstructor = true;
        return def;
    }

}

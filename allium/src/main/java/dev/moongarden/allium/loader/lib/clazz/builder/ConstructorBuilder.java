package dev.moongarden.allium.loader.lib.clazz.builder;

import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.loader.lib.clazz.definition.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

@LuaWrapped
public class ConstructorBuilder {
    private final ClassBuilder classBuilder;
    private final Supplier<List<ExecutableReference>> definitions;
    private final EClass<?> parentClass;

    private int access = 0;
    private boolean definesFields = false;
    private ConstructorReference thisDef = null;
    private EConstructor<?> superDef = null;
    private LuaFunction remapper = null;
    private List<EClass<?>> params = null;
    private String index;


    public ConstructorBuilder(ClassBuilder classBuilder, Supplier<List<ExecutableReference>> definitions, EClass<?> parentClass) {
        this.classBuilder = classBuilder;
        this.definitions = definitions;
        this.parentClass = parentClass;
    }

    @LuaWrapped
    public ConstructorBuilder index(String index) {
        this.index = index;
        return this;
    }

    @LuaWrapped
    public ConstructorBuilder access(Map<String, Boolean> access) throws ClassBuildException {
        this.access = ClassBuilder.handleMethodAccess(access);
        if ((classBuilder.access & ACC_INTERFACE) == ACC_INTERFACE) {
            throw new ClassBuildException("Interfaces can not contain a constructor");
        } else if ((this.access & ACC_ABSTRACT) == ACC_ABSTRACT) {
            throw new ClassBuildException("Constructors can not be abstract");
        }
        return this;
    }

    @LuaWrapped(name = "this")
    public ConstructorBuilder usingThis(List<EClass<?>> parameters) throws ClassBuildException {
        ConstructorReference def = testDefinitions(parameters);
        if (def == null) throw new ClassBuildException("No such constructor matching parameters.");
        this.thisDef = def;
        return this;
    }

    private ConstructorReference testDefinitions(List<EClass<?>> parameters) {
        List<ExecutableReference> definitions = this.definitions.get();
        for (ExecutableReference definition : definitions) {
            if (testDefinition(parameters, definition) && definition instanceof ConstructorReference ctorDef) {
                return ctorDef;
            }
        }
        return null;
    }

    private static boolean testDefinition(List<EClass<?>> parameters, ExecutableReference definition) {
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
    public ConstructorBuilder usingSuper(List<EClass<?>> parameters) throws ClassBuildException {
        List<EConstructor<?>> ctors = parentClass.constructors().stream()
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
        throw new ClassBuildException("No such super constructor matching parameters.");
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
    public ConstructorBuilder definesFields() {
        this.definesFields = true;
        return this;
    }

    @LuaWrapped
    public ClassBuilder build() throws ClassBuildException {
        if (params != null && remapper == null && (thisDef != null || superDef != null)) {
            throw new ClassBuildException(
                "Expected remapper function for constructor with either 'this' or 'super' constructors defined."
            );
        }

        ConstructorReference def;
        if (thisDef != null) {
            def = new ThisConstructorReference(
                thisDef, remapper, classBuilder.className,
                index,
                params == null ?
                    thisDef.params() :
                    params.stream().map((ec) -> new WrappedType(ec, ec)).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else if (superDef != null) {
            def = new SuperConstructorReference(
                superDef, remapper,
                index,
                (params == null ?
                    superDef.parameters().stream().map(WrappedType::fromParameter) :
                    params.stream().map((ec) -> new WrappedType(ec, ec))
                ).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else if (params != null && parentClass.constructor() != null) {
            def = new SuperConstructorReference(
                parentClass.constructor(), remapper,
                index,
                params.stream().map((ec) -> new WrappedType(ec, ec)).toArray(WrappedType[]::new),
                access,
                definesFields
            );
        } else {

            throw new ClassBuildException("Invalid constructor definition.");
        }

        classBuilder.apply(def);
        classBuilder.definesValidConstructor = true;
        return classBuilder;
    }

}

package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.definition.MethodDefinition;
import dev.hugeblank.allium.loader.lib.clazz.definition.WrappedType;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class MethodBuilder {
    public static final EClass<?> VOID = EClass.fromJava(void.class);

    private final ClassBuilder classBuilder;

    private int access = 0;
    private WrappedType[] params = new WrappedType[]{};
    private String name = null;
    private EClass<?> returnType = MethodBuilder.VOID;
    private boolean override = false;

    public MethodBuilder(ClassBuilder classBuilder) {
        this.classBuilder = classBuilder;
    }

    @LuaWrapped
    public MethodBuilder override(String name, List<EClass<?>> parameters) {
        var methods = new ArrayList<EMethod>();
        PropertyResolver.collectMethods(
            classBuilder.methods.stream()
                .filter((m) -> !m.isPrivate() && !m.isStatic())
                .toList(),
            name,
            methods::add
        );

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
                    override = true;
                    this.name = name;
                    access = method.modifiers() & ~ACC_ABSTRACT;
                    params = methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new);
//                    methodDefinitions.put(method.name(), new MethodDefinition(method.name(),
                        methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                        new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
//                        method.modifiers() & ~ACC_ABSTRACT
//                    ));
                    return this;
                }
            }
        }
        throw new IllegalStateException("No such override-able method '" + name + "' exists on class.");
    }

    public MethodBuilder name(String name) {
        if (override) throw new IllegalStateException("Cannot modify name of method override.");
        this.name = name;
        return this;
    }

    @LuaWrapped
    public MethodBuilder access(Map<String, Boolean> access) {
        int accessInt = ClassBuilder.handleMethodAccess(access);
        if (override && (accessInt & ACC_STATIC) == ACC_STATIC) {
            throw new IllegalStateException("Cannot change access of method override to static.");
        } else if (override && (accessInt & ACC_ABSTRACT) == ACC_ABSTRACT) {
            throw new IllegalStateException("Cannot change access of method override to abstract.");
        } else if ((classBuilder.access & ACC_INTERFACE) == ACC_INTERFACE && (accessInt & ACC_ABSTRACT) != ACC_ABSTRACT) {
            throw new IllegalStateException("Interfaces can not contain a non-abstract method");
        }
        this.access = accessInt;
        return this;
    }

    @LuaWrapped
    public MethodBuilder parameters(List<EClass<?>> parameters) {
        if (override) throw new IllegalStateException("Cannot modify parameters of method override.");
        this.params = parameters.stream().map((ec) -> new WrappedType(ec, ec.type())).toArray(WrappedType[]::new);
        return this;
    }

    @LuaWrapped
    public void returnType(EClass<?> returnType) {
        this.returnType = returnType;
    }
}

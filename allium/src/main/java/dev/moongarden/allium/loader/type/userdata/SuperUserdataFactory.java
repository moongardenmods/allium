package dev.moongarden.allium.loader.type.userdata;

import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.property.MemberFilter;
import dev.moongarden.allium.util.AnnotationUtils;
import dev.moongarden.allium.util.ArgumentUtils;
import dev.moongarden.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SuperUserdataFactory<T> extends AbstractUserdataFactory<T, SuperUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, SuperUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    // TODO: Provide a way to access protected static variables
    // Indexing a name that can't exist on the java side. Maybe there's a better way to do this?
//        if (name.equals("static")) {
//            return StaticBinder.bindClass(superClass, MemberFilter.STATIC_CHILD_MEMBER_ACCESS);
//        }

    SuperUserdataFactory(EClass<T> clazz) {
        super(clazz, clazz.superclass(), MemberFilter.CHILD_MEMBER_ACCESS);
    }

    @Override
    protected LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = super.createMetatable(isBound);
        metatable.rawset("__call", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError {
                T instance = JavaHelpers.checkUserdata(args.arg(1), clazz.raw());
                List<String> paramList = new ArrayList<>();
                for (var constructor : clazz.constructors()) {
                    if (AnnotationUtils.isHiddenFromLua(constructor)) continue;
                    var parameters = constructor.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, args, 2, parameters, List.of());

                        try {
                            Class<?> superClass = clazz.superclass().raw();
                            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(superClass, MethodHandles.lookup());
                            MethodHandle handle = lookup.findConstructor(
                                    superClass,
                                    MethodType.methodType(
                                            Void.class,
                                            parameters.stream()
                                                    .map((p) -> p.rawParameterType().raw())
                                                    .toArray(Class[]::new)
                                    )
                            );
                            List<Object> params = new ArrayList<>(List.of(jargs));
                            params.addFirst(instance);
                            try {
                                handle.invoke(params.toArray());
                                return Constants.NIL;
                            } catch (Throwable e) {
                                throw new InvocationTargetException(e);
                            }
                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                            throw new LuaError(e);
                        }
                    } catch (InvalidArgumentException e) {
                        paramList.add(ArgumentUtils.paramsToPrettyString(parameters));
                    }
                }

                StringBuilder error = new StringBuilder("Could not find parameter match for called constructor " +
                        clazz.name() +
                        "\nThe following are correct argument types:\n"
                );

                for (String headers : paramList) {
                    error.append(headers).append("\n");
                }

                throw new LuaError(error.toString());
            }
        });
        return metatable;
    }

    @Override
    public SuperUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new SuperUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public SuperUserdata<T> createBound(Object instance) {
        super.initBound();

        //noinspection unchecked
        return new SuperUserdata<>((T) instance, boundMetatable, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> SuperUserdataFactory<T> from(EClass<T> clazz) {
        return (SuperUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, SuperUserdataFactory::new);
    }
}

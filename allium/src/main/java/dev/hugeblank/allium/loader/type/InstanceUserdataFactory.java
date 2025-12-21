// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.EmptyData;
import dev.hugeblank.allium.loader.type.property.MemberFilter;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import dev.hugeblank.allium.util.MetatableUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstanceUserdataFactory<T> extends AbstractUserdataFactory<T, AlliumInstanceUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, InstanceUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();
    private @Nullable LuaTable boundMetatable;

    protected InstanceUserdataFactory(EClass<T> clazz) {
        super(clazz);
    }

    @Override
    List<EMethod> collectMetamethodCandidates() {
        return clazz.methods();
    }

    @Override
    protected LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = new LuaTable();

        metatable.rawset("__tostring", new VarArgFunction() {

            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                try {
                    // TODO: Can this be reduced to `args.arg(1).toString()`?
                    return TypeCoercions.toLuaValue(Objects.requireNonNull(TypeCoercions.toJava(state, args.arg(1), clazz)).toString());
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        MetatableUtils.applyPairs(metatable, clazz, cachedProperties, isBound, MemberFilter.PUBLIC_MEMBERS);

        metatable.rawset("__index", new VarArgFunction() {

            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
//                if (args.arg(2) instanceof LuaTable generics) {
//                    for (int i = 1; i <= generics.size(); i++) {
//                        if (generics.rawget(i) instanceof AlliumClassUserdata<?> userdata) {
//                            genericTypes.add(userdata.toUserdata());
//                        }
//                    }
//                    return args.arg(1);
//                }
                String name = args.arg(2).checkString();

                if (name.equals("super") && args.arg(1) instanceof AlliumInstanceUserdata<?> instance) {
                    return instance.superInstance();
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, MemberFilter.PUBLIC_MEMBERS);

                    cachedProperties.put(name, cachedProperty);
                }
                if (cachedProperty == EmptyData.INSTANCE) {
                    LuaValue output = MetatableUtils.getIndexMetamethod(clazz, indexImpl, state, args.arg(1), args.arg(2));
                    if (output != null) {
                        return output;
                    }
                }

                return cachedProperty.get(name, state, JavaHelpers.checkUserdata(args.arg(1), clazz.raw()), isBound);
            }
        });

        metatable.rawset("__newindex", new VarArgFunction() {
            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
                String name = args.arg(2).checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, MemberFilter.PUBLIC_MEMBERS);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && newIndexImpl != null) {
                    var parameters = newIndexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, args.subargs(2), 1, parameters, List.of());

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = TypeCoercions.toJava(state, args.arg(1), clazz);
                                newIndexImpl.invoke(instance, jargs);
                                return Constants.NIL;
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                if (e.getTargetException() instanceof LuaError err)
                                    throw err;

                                throw new LuaError(e);
                            }
                        }
                    } catch (InvalidArgumentException | IllegalArgumentException e) {
                        // Continue.
                    }
                }
                cachedProperty.set(name, state, JavaHelpers.checkUserdata(args.arg(1), clazz.raw()), args.arg(3));

                return Constants.NIL;
            }
        });

        var comparableInst = clazz.allInterfaces().stream().filter(x -> x.raw() == Comparable.class).findFirst().orElse(null);
        if (comparableInst != null) {
            var bound = comparableInst.typeVariableValues().getFirst().lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }

        return metatable;
    }

    @SuppressWarnings("unchecked")
    public static <T> InstanceUserdataFactory<T> from(EClass<T> clazz) {
        return (InstanceUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, InstanceUserdataFactory::new);
    }

    @Override
    public AlliumInstanceUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new AlliumInstanceUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public AlliumInstanceUserdata<T> createBound(Object instance) {
        if (boundMetatable == null)
            boundMetatable = createMetatable(true);

        //noinspection unchecked
        return new AlliumInstanceUserdata<>((T) instance, boundMetatable, clazz);
    }

}

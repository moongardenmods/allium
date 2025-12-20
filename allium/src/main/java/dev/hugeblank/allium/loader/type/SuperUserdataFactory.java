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
import me.basiqueevangelist.enhancedreflection.api.ModifierHolder;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SuperUserdataFactory<T> extends AbstractUserdataFactory<T, AlliumSuperUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, SuperUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    private @Nullable LuaTable boundMetatable;

    SuperUserdataFactory(EClass<T> clazz) {
        super(clazz);
    }

    @Override
    List<EMethod> collectMetamethodCandidates() {
        EClass<? super T> superClass = clazz.superclass();
        List<EMethod> targets = new ArrayList<>(superClass.methods());
        targets.addAll(superClass.declaredMethods().stream().filter(ModifierHolder::isProtected).toList());
        return targets;
    }

    @Override
    LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = new LuaTable();
        EClass<? super T> superClass = clazz.superclass();

        metatable.rawset("__tostring", new VarArgFunction() {

            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                try {
                    // TODO: Can this be reduced to `args.arg(1).toString()`?
                    return TypeCoercions.toLuaValue(Objects.requireNonNull(TypeCoercions.toJava(state, args.arg(1), superClass)).toString());
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        MetatableUtils.applyPairs(metatable, superClass, cachedProperties, isBound, MemberFilter.CHILD_MEMBER_ACCESS);

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

                // Indexing a name that can't exist on the java side. Maybe there's a better way to do this?
                if (name.equals("static")) {
                    return StaticBinder.bindClass(superClass, MemberFilter.STATIC_CHILD_MEMBER_ACCESS);
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(superClass, name, MemberFilter.CHILD_MEMBER_ACCESS);

                    cachedProperties.put(name, cachedProperty);
                }
                if (cachedProperty == EmptyData.INSTANCE) {
                    LuaValue output = MetatableUtils.getIndexMetamethod(superClass, indexImpl, state, args.arg(1), args.arg(2));
                    if (output != null) {
                        return output;
                    }
                }

                return cachedProperty.get(name, state, JavaHelpers.checkUserdata(args.arg(1), superClass.raw()), isBound);
            }
        });

        metatable.rawset("__newindex", new VarArgFunction() {
            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
                String name = args.arg(2).checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(superClass, name, MemberFilter.CHILD_MEMBER_ACCESS);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && newIndexImpl != null) {
                    var parameters = newIndexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, ValueFactory.varargsOf(args.arg(1), args.arg(2)), 1, parameters, List.of());

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = TypeCoercions.toJava(state, args.arg(1), superClass);
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
                cachedProperty.set(name, state, JavaHelpers.checkUserdata(args.arg(1), superClass.raw()), args.arg(3));

                return Constants.NIL;
            }
        });

        var comparableInst = superClass.allInterfaces().stream().filter(x -> x.raw() == Comparable.class).findFirst().orElse(null);
        if (comparableInst != null) {
            var bound = comparableInst.typeVariableValues().getFirst().lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }

        return metatable;
    }

    @Override
    public AlliumSuperUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new AlliumSuperUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public AlliumSuperUserdata<T> createBound(Object instance) {
        if (boundMetatable == null)
            boundMetatable = createMetatable(true);

        //noinspection unchecked
        return new AlliumSuperUserdata<>((T) instance, boundMetatable, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> SuperUserdataFactory<T> from(EClass<T> clazz) {
        return (SuperUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, SuperUserdataFactory::new);
    }
}

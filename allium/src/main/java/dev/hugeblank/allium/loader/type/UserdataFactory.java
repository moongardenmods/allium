// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.util.AnnotationUtils;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import dev.hugeblank.allium.util.MetatableUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.property.EmptyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserdataFactory<T> {
    private static final ConcurrentMap<EClass<?>, UserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();
    private final Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
    private final EClass<T> clazz;
    private final LuaTable metatable;
    private @Nullable LuaTable boundMetatable;
    private final @Nullable EMethod indexImpl;
    private final @Nullable EMethod newIndexImpl;

    protected UserdataFactory(EClass<T> clazz) {
        this.clazz = clazz;
        this.indexImpl = tryFindOp(LuaIndex.class, 1, "get");
        this.newIndexImpl = tryFindOp(null, 2, "set", "put");
        this.metatable = createMetatable(false);
    }

    private @Nullable EMethod tryFindOp(@Nullable Class<? extends Annotation> annotation, int minParams, String... specialNames) {
        EMethod method = null;

        if (annotation != null)
            method = clazz
                .methods()
                .stream()
                .filter(x ->
                    !x.isStatic()
                 && x.hasAnnotation(annotation))
                .findAny()
                .orElse(null);

        if (method != null) return  method;

        method = clazz
            .methods()
            .stream()
            .filter(x ->
                !x.isStatic()
             && !AnnotationUtils.isHiddenFromLua(x)
             && ArrayUtils.contains(specialNames, x.name())
             && x.parameters().size() >= minParams)
            .findAny()
            .orElse(null);

        return method;
    }

    private LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = new LuaTable();

        metatable.rawset("__tostring", new VarArgFunction() {

            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                try {
                    return TypeCoercions.toLuaValue(Objects.requireNonNull(TypeCoercions.toJava(state, args.arg(1), clazz)).toString());
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            }
        });

        MetatableUtils.applyPairs(metatable, clazz, cachedProperties, isBound);

        metatable.rawset("__index", new VarArgFunction() {

            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
                String name = args.arg(2).checkString(); // mapped name

                if (name.equals("allium_java_class")) {
                    return UserdataFactory.of(EClass.fromJava(EClass.class)).create(clazz);
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(state, clazz, name, false);

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
                String name = args.arg(2).checkString(); // mapped name

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(state, clazz, name, false);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && newIndexImpl != null) {
                    var parameters = newIndexImpl.parameters();
                    try {
                        var jargs = ArgumentUtils.toJavaArguments(state, ValueFactory.varargsOf(args.arg(1), args.arg(2)), 1, parameters);

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
            var bound = comparableInst.typeVariableValues().get(0).lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }

        return metatable;
    }

    @SuppressWarnings("unchecked")
    public static <T> UserdataFactory<T> of(EClass<T> clazz) {
        return (UserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, UserdataFactory::new);
    }

    @SuppressWarnings("unchecked")
    public static <T> AlliumUserdata<T> getUserData(T instance) {
        return (AlliumUserdata<T>) FACTORIES.computeIfAbsent(EClass.fromJava(instance.getClass()), UserdataFactory::new).create(instance);
    }

    public AlliumUserdata<T> create(Object instance) {
        return new AlliumUserdata<>(instance, metatable, clazz);
    }

    public AlliumUserdata<T> createBound(Object instance) {
        if (boundMetatable == null)
            boundMetatable = createMetatable(true);

        return new AlliumUserdata<>(instance, boundMetatable, clazz);
    }


    private static final class LessFunction extends VarArgFunction {
        private final EClass<?> bound;

        public LessFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
            Comparable<Object> cmp = JavaHelpers.checkUserdata(args.arg(1), Comparable.class);
            Object cmp2 = JavaHelpers.checkUserdata(args.arg(2), bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0);
        }
    }

    private static final class LessOrEqualFunction extends VarArgFunction {
        private final EClass<?> bound;

        public LessOrEqualFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
            Comparable<Object> cmp = JavaHelpers.checkUserdata(args.arg(1), Comparable.class);
            Object cmp2 = JavaHelpers.checkUserdata(args.arg(2), bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0 || cmp.equals(cmp2));
        }
    }
}

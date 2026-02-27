package dev.moongarden.allium.loader.type.userdata;

import dev.moongarden.allium.loader.type.coercion.TypeCoercions;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.property.CustomData;
import dev.moongarden.allium.loader.type.property.EmptyData;
import dev.moongarden.allium.loader.type.property.PropertyData;
import dev.moongarden.allium.loader.type.property.PropertyResolver;
import dev.moongarden.allium.util.JavaHelpers;
import dev.moongarden.allium.util.MetatableUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ArrayUserdataFactory<T> extends AbstractUserdataFactory<T, ArrayUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, ArrayUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    protected ArrayUserdataFactory(EClass<T> clazz) {
        super(clazz);
    }

    @Override
    protected LibFunction __len(boolean isBound) {
        return LibFunction.create((state, userdata) -> {
            PropertyData<? super T> cachedProperty = cachedProperties.get("length");
            if (cachedProperty == null && userdata instanceof ArrayUserdata<?> array) {
                cachedProperty = new CustomData<>(() -> TypeCoercions.toLuaValue(Array.getLength(array.toUserdata()), EClass.fromJava(int.class)));
                cachedProperties.put("length", cachedProperty);
            }

            if (cachedProperty != null) {
                LuaValue out = cachedProperty.get("length", state, JavaHelpers.checkUserdata(userdata, targetClass.raw()), isBound);
                if (!out.isNil()) return out;
            }

            throw new LuaError("Metamethod for array used on non-array type.");
        });
    }

    @Override
    protected LibFunction __pairs(boolean isBound) {
        final EClass<?> INT_CLASS = EClass.fromJava(int.class);
        return LibFunction.create((_, userdata) -> {
            if (userdata instanceof ArrayUserdata<?> array) {
                final int length = Array.getLength(array.toUserdata());
                return new VarArgFunction() { // next
                    int i = 0;
                    public Varargs invoke(LuaState state, Varargs varargs) {
                        if (i == length) return Constants.NIL;
                        return ValueFactory.varargsOf(
                            TypeCoercions.toLuaValue(i+1, INT_CLASS),
                            TypeCoercions.toLuaValue(Array.get(array.toUserdata(), i++), array.instanceClass().arrayComponent())
                        );
                    }
                };
            }

            throw new LuaError("Metamethod for array used on non-array type.");
        });
    }

    @Override
    protected LibFunction __index(boolean isBound) {
        return LibFunction.create((LuaState state, LuaValue userdata, LuaValue luaIndex) -> {
            String name = luaIndex.isString() ? luaIndex.checkString() : String.valueOf(luaIndex.checkInteger());
            PropertyData<? super T> cachedProperty = cachedProperties.get(name);
            if (cachedProperty == null && userdata instanceof ArrayUserdata<?> array) {
                if (luaIndex.isString()) {
                    if (name.equals("length")) {
                        cachedProperty = new CustomData<>(() -> TypeCoercions.toLuaValue(Array.getLength(array.toUserdata()), EClass.fromJava(int.class)));
                    } else {
                        cachedProperty = EmptyData.INSTANCE;
                    }
                } else {
                    int i = luaIndex.checkInteger();
                    cachedProperty = createData(array, i);
                }
                cachedProperties.put(name, cachedProperty);
                return cachedProperty.get(name, state, JavaHelpers.checkUserdata(userdata, targetClass.raw()), isBound);
            }

            throw new LuaError("Metamethod for array used on non-array type.");
        });
    }

    @Override
    protected LibFunction __newindex(boolean isBound) {
        return LibFunction.create((LuaState state, LuaValue userdata, LuaValue luaIndex, LuaValue value) -> {
            int index = luaIndex.checkInteger();
            String name = String.valueOf(index);

            PropertyData<? super T> cachedProperty = cachedProperties.get(name);

            if (cachedProperty == null && userdata instanceof ArrayUserdata<?> array) {
                cachedProperty = createData(array, index);
                cachedProperties.put(name, cachedProperty);
            }

            if (cachedProperty != null) {
                cachedProperty.set(name, state, JavaHelpers.checkUserdata(userdata, targetClass.raw()), value);
                return Constants.NIL;
            }

            throw new LuaError("Metamethod for array used on non-array type.");
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> ArrayUserdataFactory<T> from(EClass<T> clazz) {
        return (ArrayUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, ArrayUserdataFactory::new);
    }

    @Override
    public ArrayUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new ArrayUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public ArrayUserdata<T> createBound(Object instance) {
        super.initBound();

        //noinspection unchecked
        return new ArrayUserdata<>((T) instance, boundMetatable, clazz);
    }


    public static <T> CustomData<T> createData(ArrayUserdata<?> array, int index) {
        return new CustomData<>(
            () -> TypeCoercions.toLuaValue(Array.get(array.toUserdata(), index), array.instanceClass().arrayComponent()),
            (_, _, _, value) -> Array.set(array.toUserdata(), index, TypeCoercions.toLuaValue(value, array.instanceClass().arrayComponent()))
        );
    }
}
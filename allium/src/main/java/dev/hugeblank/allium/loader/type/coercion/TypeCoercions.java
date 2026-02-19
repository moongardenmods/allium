package dev.hugeblank.allium.loader.type.coercion;

import dev.hugeblank.allium.loader.type.MethodInvocationFunction;
import dev.hugeblank.allium.api.CoerceToBound;
import dev.hugeblank.allium.api.CoerceToNative;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.userdata.ClassUserdata;
import dev.hugeblank.allium.loader.type.userdata.InstanceUserdata;
import dev.hugeblank.allium.loader.type.userdata.InstanceUserdataFactory;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.*;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TypeCoercions {
    private static final Map<Class<?>, Function<EClass<?>, LuaToJavaConverter<?>>> FROM_LUA = new HashMap<>();
    private static final Map<Class<?>, BiFunction<Object, EClassUse<?>, JavaToLuaConverter<?>>> TO_LUA = new HashMap<>();

    public static <T> void registerJavaToLua(Class<T> klass, JavaToLuaConverter<T> serializer) {
        if (TO_LUA.put(klass, (_, ignored) -> serializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerComplexJavaToLua(Class<T> klass, BiFunction<T, EClassUse<T>, JavaToLuaConverter<T>> serializerFactory) {
        if (TO_LUA.put(klass, (a, b) -> serializerFactory.apply((T)a, (EClassUse<T>) b)) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    public static <T> void registerLuaToJava(Class<T> klass, LuaToJavaConverter<T> deserializer) {
        if (FROM_LUA.put(klass, _ -> deserializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerLuaToJava(Class<T> klass, Function<EClass<T>, LuaToJavaConverter<T>> deserializerFactory) {
        if (FROM_LUA.put(klass, (Function<EClass<?>, LuaToJavaConverter<?>>)(Object) deserializerFactory) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    public static Object toJava(LuaState state, LuaValue value, Class<?> clazz) throws InvalidArgumentException, LuaError {
        return toJava(state, value, EClass.fromJava(clazz));
    }

    public static Object toJava(LuaState state, LuaValue value, EClass<?> clazz) throws LuaError, InvalidArgumentException {
        if (clazz.isAssignableFrom(value.getClass()) && !clazz.equals(CommonTypes.OBJECT)) {
            return value;
        }

        if (value.isNil())
            return null;

        if (value instanceof InstanceUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clazz.wrapPrimitive());
            } catch (ClassCastException e) {
                throw new InvalidArgumentException(e);
            }
        } else if (value instanceof ClassUserdata<?> userdata) {
            return userdata.toUserdata();
        }

        var deserializerFactory = FROM_LUA.get(clazz.raw());
        if (deserializerFactory != null) {
            var deserializer = deserializerFactory.apply(clazz);

            if (deserializer != null) {
                Object result = deserializer.fromLua(state, value);

                if (result != null) return result;
            }
        }

        if (clazz.type() == ClassType.ARRAY) {
            try {
                LuaTable table = value.checkTable();
                int length = table.length();
                Object arr = Array.newInstance(clazz.arrayComponent().raw(), table.length());
                for (int i = 0; i < length; i++) {
                    Array.set(arr, i, toJava(state, table.rawget(i + 1), clazz.arrayComponent()));
                }
                return clazz.cast(arr);
            } catch (Exception e) {
                throw new LuaError(
                        "Expected table of "
                                + clazz.arrayComponent()
                                + "s, got "
                                + value.typeName()
                );
            }
        }

        if (value instanceof LuaFunction func && clazz.type() == ClassType.INTERFACE) { // Callbacks
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : clazz.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return ProxyGenerator.getProxyFactory(clazz, ifaceMethod).apply(state, func);
            }
        }

        throw new InvalidArgumentException("Couldn't convert " + value + " to java! Target type is " + clazz);
    }

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? EClass.fromJava(out.getClass()) : CommonTypes.OBJECT);
    }

    public static LuaValue toLuaValue(Object out, EClass<?> ret) {
        return toLuaValue(out, ret.asEmptyUse());
    }

    public static LuaValue toLuaValue(Object out, EClass<?> ret, boolean unwrapPrimitives) {
        return toLuaValue(out, ret.asEmptyUse(), unwrapPrimitives);
    }

    public static LuaValue toLuaValue(Object out, EClassUse<?> ret) {
        return toLuaValue(out, ret, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static LuaValue toLuaValue(Object out, EClassUse<?> ret, boolean unwrapPrimitives) {
        EClass<?> klass = ret.type();

        if (unwrapPrimitives) klass = klass.unwrapPrimitive();

        if (out == null) {
            return Constants.NIL;
        } else if (out instanceof LuaValue) {
            return (LuaValue) out;
        }

        var serializerFactory = TO_LUA.get(klass.raw());
        if (serializerFactory != null) {
            var serializer = (JavaToLuaConverter<Object>) serializerFactory.apply(out, ret);

            if (serializer != null) {
                LuaValue result = serializer.toLua(out);

                if (result != null) return result;
            }
        }

        if (klass.type() == ClassType.ARRAY) {
            var table = new LuaTable();
            int length = Array.getLength(out);
            for (int i = 1; i <= length; i++) {
                table.rawset(i, toLuaValue(Array.get(out, i - 1), ret.arrayComponent()));
            }
            return table;
        } else if (klass.type() == ClassType.INTERFACE && klass.hasAnnotation(FunctionalInterface.class)) {
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : klass.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return new MethodInvocationFunction(klass, Collections.singletonList(ifaceMethod), ifaceMethod.name(), out, false);
            } else {
                return InstanceUserdataFactory.from(klass).create(klass.cast(out));
            }
        } else if (klass.raw().isAssignableFrom(out.getClass())) {
            EClass<?> trueRet = EClass.fromJava(out.getClass());

            if (canMatch(trueRet, klass)) {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return InstanceUserdataFactory.from(trueRet).createBound(out);
                else
                    return InstanceUserdataFactory.from(trueRet).create(out);
            } else {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return InstanceUserdataFactory.from(klass).createBound(klass.cast(out));
                else
                    return InstanceUserdataFactory.from(klass).create(klass.cast(out));
            }
        } else {
            return Constants.NIL;
        }
    }

    private static boolean canMatch(EType type, EType other) {
        if (type.equals(other)) return true;

        if (type instanceof EClass<?> klass) {
            if (other instanceof EWildcard otherWildcard) {
                return otherWildcard.upperBounds().stream().allMatch(x -> canMatch(klass, x))
                    && otherWildcard.lowerBounds().stream().noneMatch(x -> canMatch(klass, x));
            } else if (other instanceof EClass<?> otherKlass) {
                if (otherKlass.raw().equals(klass.raw())) {
                    for (int i = 0; i < otherKlass.typeVariableValues().size(); i++)  {
                        var val = klass.typeVariableValues().get(i);
                        var otherVal = otherKlass.typeVariableValues().get(i);

                        if (otherVal instanceof EWildcard && canMatch(val, otherVal))
                            return true;
                    }
                }
            }

            if (klass.allSuperclasses().stream().anyMatch(x -> canMatch(x, other)))
                return true;

            return klass.interfaces().stream().anyMatch(x -> canMatch(x, other));
        }

        return false;
    }

    static {
        TypeCoercions.registerJavaToLua(int.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(byte.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(short.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(char.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(double.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(float.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(long.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(boolean.class, ValueFactory::valueOf);
        TypeCoercions.registerJavaToLua(String.class, ValueFactory::valueOf);

        TypeCoercions.registerComplexJavaToLua(Collection.class, (_, use) -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> componentUse = use.typeVariableValues().getFirst().upperBound();

            return list -> {
                LuaTable table = new LuaTable();
                int i = 1;
                for (Object o : list) {
                    table.rawset(i, toLuaValue(o, componentUse));
                }
                return table;
            };
        });

        TypeCoercions.registerComplexJavaToLua(Map.class, (_, use) -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> keyUse = use.typeVariableValues().get(0).upperBound();
            EClassUse<?> valueUse = use.typeVariableValues().get(1).upperBound();

            return map -> {
                LuaTable table = new LuaTable();

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                    Object key = entry.getKey();
                    table.rawsetImpl(
                            TypeCoercions.toLuaValue(
                                    key,
                                    keyUse
                            ),
                            TypeCoercions.toLuaValue(entry.getValue(), valueUse)
                    );
                }

                return table;
            };
        });

        TypeCoercions.registerLuaToJava(boolean.class, (_, val) -> suppressError(val::checkBoolean));
        TypeCoercions.registerLuaToJava(byte.class, (_, val) -> suppressError(() -> (byte)val.checkInteger()));
        TypeCoercions.registerLuaToJava(short.class, (_, val) -> suppressError(() -> (short)val.checkInteger()));
        TypeCoercions.registerLuaToJava(int.class, (_, val) -> suppressError(val::checkInteger));
        TypeCoercions.registerLuaToJava(long.class, (_, val) -> suppressError(val::checkLong));
        TypeCoercions.registerLuaToJava(float.class, (_, val) -> suppressError(() -> (float)val.checkDouble()));
        TypeCoercions.registerLuaToJava(double.class, (_, val) -> suppressError(val::checkDouble));
        TypeCoercions.registerLuaToJava(char.class, (_, val) -> suppressError(() -> {
            if (val.isNumber()) {
                return (char) val.checkInteger();
            } else {
                return val.checkString().charAt(0);
            }
        }));
        TypeCoercions.registerLuaToJava(Boolean.class, (_, val) -> suppressError(val::checkBoolean));
        TypeCoercions.registerLuaToJava(Byte.class, (_, val) -> suppressError(() -> (byte)val.checkInteger()));
        TypeCoercions.registerLuaToJava(Short.class, (_, val) -> suppressError(() -> (short)val.checkInteger()));
        TypeCoercions.registerLuaToJava(Integer.class, (_, val) -> suppressError(val::checkInteger));
        TypeCoercions.registerLuaToJava(Long.class, (_, val) -> suppressError(val::checkLong));
        TypeCoercions.registerLuaToJava(Float.class, (_, val) -> suppressError(() -> (float)val.checkDouble()));
        TypeCoercions.registerLuaToJava(Double.class, (_, val) -> suppressError(val::checkDouble));
        TypeCoercions.registerLuaToJava(Character.class, (_, val) -> suppressError(() -> {
            if (val.isString()) {
                return val.checkString().charAt(0);
            } else {
                return (char)val.checkInteger();
            }
        }));
        TypeCoercions.registerLuaToJava(String.class, (_, val) -> suppressError(val::checkString));

        TypeCoercions.registerLuaToJava(Class.class, (_, val) -> {
            EClass<?> klass = JavaHelpers.asClass(val);
            if (klass == null) return null;
            else return klass.raw();
        });

        registerCollection(Collection.class, ArrayList::new);
        registerCollection(SequencedCollection.class, ArrayList::new);
        registerCollection(List.class, ArrayList::new);
        registerCollection(Set.class, HashSet::new);
        registerCollection(Queue.class, ArrayDeque::new);
        registerCollection(Deque.class, ArrayDeque::new);
        registerCollection(Vector.class, Vector::new);
        registerCollection(Stack.class, (_) -> new Stack<>());


        registerMap(Map.class, HashMap::new);
        registerMap(SequencedMap.class, LinkedHashMap::new);
        registerMap(SortedMap.class, (_) -> new TreeMap<>());
        registerMap(NavigableMap.class, (_) -> new TreeMap<>());

        // More collection and map types are welcome
    }

    static <U> void registerCollection(Class<U> clazz, Function<Integer, Collection<Object>> initializer) {
        TypeCoercions.registerLuaToJava(clazz, klass -> {
            EClass<?> componentType = klass.typeVariableValues().getFirst().upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();

                Collection<Object> list = initializer.apply(length);

                for (int i = 0; i < length; i++) {
                    list.add(TypeCoercions.toJava(state, table.rawget(i + 1), componentType));
                }

                //noinspection unchecked
                return (U) list;
            };
        });
    }

    static <U> void registerMap(Class<U> clazz, Function<Integer, Map<Object, Object>> initializer) {
        TypeCoercions.registerLuaToJava(clazz, klass -> {
            if (!klass.raw().equals(Map.class)) return null; // Maybe eventually support other classes that inherit Map.
            EClass<?> keyType = klass.typeVariableValues().get(0).upperBound();
            EClass<?> valueType = klass.typeVariableValues().get(1).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                Map<Object, Object> map = initializer.apply(length);

                LuaValue k = Constants.NIL;
                while (true) {
                    Varargs n = table.next(k);
                    if ((k = n.arg(1)).isNil())
                        break;
                    LuaValue v = n.arg(2);

                    map.put(TypeCoercions.toJava(state, k, keyType), TypeCoercions.toJava(state, v, valueType));
                }

                //noinspection unchecked
                return (U) map;
            };
        });
    }

    private static <T> T suppressError(SupplierThrowsLuaError<T> checkValue) {
        try {
            return checkValue.get();
        } catch (LuaError ignored) {}
        return null;
    }

    private interface SupplierThrowsLuaError<T> {
        T get() throws LuaError;
    }

}

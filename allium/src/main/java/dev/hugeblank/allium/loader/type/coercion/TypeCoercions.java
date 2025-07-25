package dev.hugeblank.allium.loader.type.coercion;

import dev.hugeblank.allium.loader.type.*;
import dev.hugeblank.allium.loader.type.annotation.CoerceToBound;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.*;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LuaFunction;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;

public class TypeCoercions {
    private static final Map<Class<?>, Function<EClass<?>, LuaToJavaConverter<?>>> FROM_LUA = new HashMap<>();
    private static final Map<Class<?>, Function<EClassUse<?>, JavaToLuaConverter<?>>> TO_LUA = new HashMap<>();

    public static <T> void registerJavaToLua(Class<T> klass, JavaToLuaConverter<T> serializer) {
        if (TO_LUA.put(klass, unused -> serializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerComplexJavaToLua(Class<T> klass, Function<EClassUse<T>, JavaToLuaConverter<T>> serializerFactory) {
        if (TO_LUA.put(klass, (Function<EClassUse<?>, JavaToLuaConverter<?>>)(Object) serializerFactory) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    public static <T> void registerLuaToJava(Class<T> klass, LuaToJavaConverter<T> deserializer) {
        if (FROM_LUA.put(klass, unused -> deserializer) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerLuaToJava(Class<T> klass, Function<EClass<T>, LuaToJavaConverter<T>> deserializerFactory) {
        if (FROM_LUA.put(klass, (Function<EClass<?>, LuaToJavaConverter<?>>)(Object) deserializerFactory) != null)
            throw new IllegalStateException("Converter already registered for " + klass);
    }

    public static Object toJava(LuaState state, LuaValue value, Class<?> clatz) throws InvalidArgumentException, LuaError {
        return toJava(state, value, EClass.fromJava(clatz));
    }

    public static Object toJava(LuaState state, LuaValue value, EClass<?> clatz) throws LuaError, InvalidArgumentException {
        if (clatz.isAssignableFrom(value.getClass()) && !clatz.equals(CommonTypes.OBJECT)) {
            return value;
        }

        if (value.isNil())
            return null;
        
        if (value instanceof AlliumInstanceUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clatz.wrapPrimitive());
            } catch (ClassCastException e) {
                throw new InvalidArgumentException(e);
            }
        } else if (value instanceof AlliumClassUserdata<?> userdata) {
            return userdata.toUserdata();
        }

        clatz = clatz.unwrapPrimitive();

        var deserializerFactory = FROM_LUA.get(clatz.raw());
        if (deserializerFactory != null) {
            var deserializer = deserializerFactory.apply(clatz);

            if (deserializer != null) {
                Object result = deserializer.fromLua(state, value);

                if (result != null) return result;
            }
        }

        if (clatz.type() == ClassType.ARRAY) {
            try {
                LuaTable table = value.checkTable();
                int length = table.length();
                Object arr = Array.newInstance(clatz.arrayComponent().raw(), table.length());
                for (int i = 0; i < length; i++) {
                    Array.set(arr, i, toJava(state, table.rawget(i + 1), clatz.arrayComponent()));
                }
                return clatz.cast(arr);
            } catch (Exception e) {
                throw new LuaError(
                        "Expected table of "
                                + clatz.arrayComponent()
                                + "s, got "
                                + value.typeName()
                );
            }
        }

        if (value instanceof LuaFunction func && clatz.type() == ClassType.INTERFACE) { // Callbacks
            EMethod ifaceMethod = null;

            int unimplemented = 0;
            for (var meth : clatz.methods()) {
                if (meth.isAbstract()) {
                    unimplemented++;
                    ifaceMethod = meth;

                    if (unimplemented > 1) {
                        break;
                    }
                }
            }

            if (unimplemented == 1) {
                return ProxyGenerator.getProxyFactory(clatz, ifaceMethod).apply(state, func);
            } // TODO: Weird code was removed here. Did that break anything?
        }

        throw new InvalidArgumentException("Couldn't convert " + value + " to java! Target type is " + clatz);
    }

    public static LuaValue toLuaValue(Object out) {
        return toLuaValue(out, out != null ? EClass.fromJava(out.getClass()) : CommonTypes.OBJECT);
    }

    public static LuaValue toLuaValue(Object out, EClass<?> ret) {
        return toLuaValue(out, ret.asEmptyUse());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static LuaValue toLuaValue(Object out, EClassUse<?> ret) {
        EClass<?> klass = ret.type();

        klass = klass.unwrapPrimitive();

        if (out == null) {
            return Constants.NIL;
        } else if (out instanceof LuaValue) {
            return (LuaValue) out;
        }

        var serializerFactory = TO_LUA.get(klass.raw());
        if (serializerFactory != null) {
            var serializer = (JavaToLuaConverter<Object>) serializerFactory.apply(ret);

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
                return new UDFFunctions(klass, Collections.singletonList(ifaceMethod), ifaceMethod.name(), out, false);
            } else {
                return UserdataFactory.of(klass).create(klass.cast(out));
            }
        } else if (klass.raw().isAssignableFrom(out.getClass())) {
            EClass<?> trueRet = EClass.fromJava(out.getClass());

            if (canMatch(trueRet, klass)) {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(trueRet).createBound(out);
                else
                    return UserdataFactory.of(trueRet).create(out);
            } else {
                if (ret.hasAnnotation(CoerceToBound.class))
                    return UserdataFactory.of(klass).createBound(klass.cast(out));
                else
                    return UserdataFactory.of(klass).create(klass.cast(out));
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

            if (klass.interfaces().stream().anyMatch(x -> canMatch(x, other)))
                return true;
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

        TypeCoercions.registerComplexJavaToLua(List.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> componentUse = use.typeVariableValues().get(0).upperBound();

            return list -> {
                LuaTable table = new LuaTable();
                int length = list.size();

                for (int i = 0; i < length; i++) {
                    table.rawset(i + 1, toLuaValue(list.get(i), componentUse));
                }

                return table;
            };
        });

        TypeCoercions.registerComplexJavaToLua(Set.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> componentUse = use.typeVariableValues().get(0).upperBound();

            return set -> {
                LuaTable table = new LuaTable();
                int i = 0;
                for (Object o : set) {
                    table.rawset(i + 1, toLuaValue(o, componentUse));
                    i++;
                }
                return table;
            };
        });

        TypeCoercions.registerComplexJavaToLua(Map.class, use -> {
            if (!use.hasAnnotation(CoerceToNative.class)) return null;

            EClassUse<?> keyUse = use.typeVariableValues().get(0).upperBound();
            EClassUse<?> valueUse = use.typeVariableValues().get(1).upperBound();

            return map -> {
                LuaTable table = new LuaTable();

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                    table.rawsetImpl(TypeCoercions.toLuaValue(entry.getKey(), keyUse), TypeCoercions.toLuaValue(entry.getValue(), valueUse));
                }

                return table;
            };
        });

        TypeCoercions.registerLuaToJava(int.class, (state, val) -> suppressError(val::checkInteger));
        TypeCoercions.registerLuaToJava(byte.class, (state, val) -> suppressError(() -> (byte)val.checkInteger()));
        TypeCoercions.registerLuaToJava(short.class, (state, val) -> suppressError(() -> (short)val.checkInteger()));
        TypeCoercions.registerLuaToJava(char.class, (state, val) -> suppressError(() -> (char)val.checkInteger()));
        TypeCoercions.registerLuaToJava(double.class, (state, val) -> suppressError(val::checkDouble));
        TypeCoercions.registerLuaToJava(float.class, (state, val) -> suppressError(() -> (float)val.checkDouble()));
        TypeCoercions.registerLuaToJava(long.class, (state, val) -> suppressError(val::checkLong));
        TypeCoercions.registerLuaToJava(boolean.class, (state, val) -> suppressError(val::checkBoolean));
        TypeCoercions.registerLuaToJava(String.class, (state, val) -> suppressError(val::checkString));

        TypeCoercions.registerLuaToJava(EClass.class, JavaHelpers::asClass);
        TypeCoercions.registerLuaToJava(Class.class, (state, val) -> {
            EClass<?> klass = JavaHelpers.asClass(state, val);
            if (klass == null) return null;
            else return klass.raw();
        });

        TypeCoercions.registerLuaToJava(List.class, klass -> {
            EClass<?> componentType = klass.typeVariableValues().get(0).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                List<Object> list = new ArrayList<>(length);

                for (int i = 0; i < length; i++) {
                    list.add(TypeCoercions.toJava(state, table.rawget(i + 1), componentType));
                }

                return list;
            };
        });

        TypeCoercions.registerLuaToJava(Set.class, klass -> {
            EClass<?> componentType = klass.typeVariableValues().get(0).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                Set<Object> set = new HashSet<>(length);

                for (int i = 0; i < length; i++) {
                    set.add(TypeCoercions.toJava(state, table.rawget(i + 1), componentType));
                }

                return set;
            };
        });

        TypeCoercions.registerLuaToJava(Map.class, klass -> {
            EClass<?> keyType = klass.typeVariableValues().get(0).upperBound();
            EClass<?> valueType = klass.typeVariableValues().get(1).upperBound();

            return (state, value) -> {
                LuaTable table = value.checkTable();
                int length = table.length();
                Map<Object, Object> map = new HashMap<>(length);

                LuaValue k = Constants.NIL;
                while (true) {
                    Varargs n = table.next(k);
                    if ((k = n.arg(1)).isNil())
                        break;
                    LuaValue v = n.arg(2);

                    map.put(TypeCoercions.toJava(state, k, keyType), TypeCoercions.toJava(state, v, valueType));
                }

                return map;
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

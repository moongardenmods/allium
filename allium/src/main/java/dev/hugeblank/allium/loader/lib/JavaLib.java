package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.WrappedLibrary;
import dev.hugeblank.allium.loader.type.AlliumClassUserdata;
import dev.hugeblank.allium.loader.type.AlliumInstanceUserdata;
import dev.hugeblank.allium.loader.type.MethodInvocationFunction;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaArgs;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.builder.ClassBuilder;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EType;
import org.squiddev.cobalt.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@LuaWrapped(name = "java")
public class JavaLib implements WrappedLibrary {

    @LuaWrapped(name = "boolean") public static final AlliumClassUserdata<?> primitiveBoolean = StaticBinder.bindClass(EClass.fromJava(boolean.class));
    @LuaWrapped(name = "byte") public static final AlliumClassUserdata<?> primitiveByte = StaticBinder.bindClass(EClass.fromJava(byte.class));
    @LuaWrapped(name = "short") public static final AlliumClassUserdata<?> primitiveShort = StaticBinder.bindClass(EClass.fromJava(short.class));
    @LuaWrapped(name = "int") public static final AlliumClassUserdata<?> primitiveInt = StaticBinder.bindClass(EClass.fromJava(int.class));
    @LuaWrapped(name = "long") public static final AlliumClassUserdata<?> primitiveLong = StaticBinder.bindClass(EClass.fromJava(long.class));
    @LuaWrapped(name = "float") public static final AlliumClassUserdata<?> primitiveFloat = StaticBinder.bindClass(EClass.fromJava(float.class));
    @LuaWrapped(name = "double") public static final AlliumClassUserdata<?> primitiveDouble = StaticBinder.bindClass(EClass.fromJava(double.class));
    @LuaWrapped(name = "char") public static final AlliumClassUserdata<?> primitiveChar = StaticBinder.bindClass(EClass.fromJava(char.class));

    @LuaWrapped
    public static LuaValue cast(@LuaStateArg LuaState state, LuaUserdata object, EClass<?> klass) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, object, klass), klass);
        } catch (InvalidArgumentException | LuaError e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public static <T> LuaValue coerce(@LuaStateArg LuaState state, AlliumInstanceUserdata<T> tableLike, EClass<T> klass) throws LuaError {
        LuaTable out = new LuaTable();
        List<EType> typeVars = klass.typeVariableValues();
        final LuaError err = new LuaError("Could not coerce type '" + tableLike.instance.getClass().getName() + "' to '" + klass.name() + "'");
        return switch (typeVars.size()) {
            case 1 -> {
                if (tableLike.instance instanceof Collection<?> collection) {
                    EClass<?> type = typeVars.getFirst().upperBound();
                    collection.forEach((val) -> out.rawset(out.length()+1, TypeCoercions.toLuaValue(val, type)));
                    yield out;
                }
                throw err;
            }
            case 2 -> {
                if (tableLike.instance instanceof Map<?,?> map) {
                    EClass<?> keyType = typeVars.getFirst().upperBound();
                    EClass<?> valType = typeVars.getLast().upperBound();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        out.rawset(
                                TypeCoercions.toLuaValue(entry.getKey(), keyType),
                                TypeCoercions.toLuaValue(entry.getValue(), valType)
                        );
                    }
                    yield out;
                }
                throw err;
            }
            default -> throw err;
        };
    }

    @LuaWrapped
    public static LuaValue wrap(@LuaStateArg LuaState state, LuaValue value, EClass<?> klass) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, value, klass), klass, false);
        } catch (InvalidArgumentException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public static boolean instanceOf(@LuaStateArg LuaState state, LuaUserdata object, EClass<?> klass) {
        try {
            Object obj = TypeCoercions.toJava(state, object, klass);
            if (obj == null) return false;
            return klass.isAssignableFrom(obj.getClass());
        } catch (LuaError | InvalidArgumentException e) {
            return false;
        }
    }

    @LuaWrapped
    public static Varargs callWith(@LuaStateArg LuaState state, MethodInvocationFunction<?> function, List<EClass<?>> types, @LuaArgs Varargs params) throws LuaError {
        function.setForcedParameters(types);
        return function.invoke(state, params);
    }

    @LuaWrapped
    public static ClassBuilder extendClass(@LuaStateArg LuaState state, EClass<?> superclass, @OptionalArg List<EClass<?>> interfaces, @OptionalArg Map<String, Boolean> access) {
        if (interfaces == null) interfaces = List.of();
        if (access == null) access = Map.of();
        return new ClassBuilder(superclass, interfaces, access, state);
    }

    @LuaWrapped
    public static EClass<?> getRawClass(String className) throws LuaError {
        return JavaHelpers.getRawClass(className);

    }

    @LuaWrapped(name = "throw")
    public void exception(Throwable error) throws Throwable {
        throw error;
    }

}


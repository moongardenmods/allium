package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.util.JavaHelpers;
import dev.hugeblank.allium.loader.type.builder.ClassBuilder;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaUserdata;
import org.squiddev.cobalt.LuaValue;

import java.util.List;
import java.util.Map;

@LuaWrapped(name = "java")
public class JavaLib implements WrappedLuaLibrary {

    @LuaWrapped
    public static LuaValue cast(@LuaStateArg LuaState state, LuaUserdata object, EClass<?> klass) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, object, klass), klass);
        } catch (InvalidArgumentException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public static LuaValue wrap(@LuaStateArg LuaState state, LuaValue value, EClass<?> klass) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, value, klass, false), klass, false);
        } catch (InvalidArgumentException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public static boolean instanceOf(@LuaStateArg LuaState state, LuaUserdata object, EClass<?> klass) {
        try {
            Object obj = TypeCoercions.toJava(state, object, Object.class);
            return klass.isAssignableFrom(obj.getClass());
        } catch (LuaError | InvalidArgumentException e) {
            return false;
        }
    }

    @LuaWrapped
    public static boolean exists(@LuaStateArg LuaState state, String string, @OptionalArg Class<?>[] value) {
        try {
            var parts = string.split("#");
            var clazz = getRawClass(parts[0]);

            if (parts.length != 2) {
                return true;
            }

            if (value != null) {
                return clazz.method(parts[1], value) != null;
            } else {
                for (var method : clazz.methods()) {
                    if (method.name().equals(parts[1])) {
                        return true;
                    }
                }

                return clazz.field(parts[1]) != null;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @LuaWrapped
    public static ClassBuilder extendClass(@LuaStateArg LuaState state, EClass<?> superclass, @OptionalArg List<EClass<?>> interfaces, Map<String, Boolean> access) {
        if (interfaces == null) interfaces = List.of();
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


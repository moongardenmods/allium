package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.*;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;

import java.util.List;

@LuaWrapped(name = "java")
public class JavaLib implements WrappedLuaLibrary {

    @LuaWrapped
    public static String toMappings(@LuaStateArg LuaState state, String string) throws LuaError {
        return ScriptRegistry.find(state).getMappings().getMapped(string);
    }

    @LuaWrapped
    public static @CoerceToNative List<String> fromMappings(@LuaStateArg LuaState state, String string) throws LuaError {
        return ScriptRegistry.find(state).getMappings().getUnmapped(string);
    }

    @LuaWrapped
    public static LuaValue cast(@LuaStateArg LuaState state, LuaUserdata object, EClass<?> klass) throws LuaError {
        try {
            return TypeCoercions.toLuaValue(TypeCoercions.toJava(state, object, klass), klass);
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
            var clazz = getRawClass(state, parts[0]);

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
    public static ClassBuilder extendClass(@LuaStateArg LuaState state, EClass<?> superclass, List<EClass<?>> interfaces) {
        return new ClassBuilder(superclass, interfaces, state);
    }

    @LuaWrapped
    public static EClass<?> getRawClass(@LuaStateArg LuaState state, String className) throws LuaError {
        return JavaHelpers.getRawClass(state, className);

    }

}

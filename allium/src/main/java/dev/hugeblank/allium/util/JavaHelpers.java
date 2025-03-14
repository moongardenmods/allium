package dev.hugeblank.allium.util;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.AlliumUserdata;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;

public class JavaHelpers {


    public static <T> T checkUserdata(LuaValue value, Class<T> clazz) throws LuaError {
        if (value instanceof AlliumUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clazz);
            } catch (Exception e) {
                throw new LuaError(e);
            }
        }
        throw new LuaError("value " + value + " is not an instance of AlliumUserData");
    }

    public static EClass<?> getRawClass(LuaState state, String className) throws LuaError {
            try {
                className = ScriptRegistry.scriptFromState(state).getMappings().getUnmapped(className).get(0);
                return EClass.fromJava(Class.forName(className));
            } catch (ClassNotFoundException ignored) {}

            try {
                return EClass.fromJava(Class.forName(className));
            } catch (ClassNotFoundException ignored) {}

        throw new LuaError("Couldn't find class \"" + className + "\"");
    }

    public static EClass<?> asClass(LuaState state, LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(state, value.checkString());
        } else if (value.isNil()) {
            return null;
        } else if (value instanceof LuaTable table && table.rawget("allium_java_class") instanceof AlliumUserdata<?> userdata) {
            return userdata.toUserdata(EClass.class);
        } else if (value instanceof AlliumUserdata<?> userdata) {
            if (userdata.instanceOf(EClass.class)) {
                return userdata.toUserdata(EClass.class);
            } else if (userdata.instanceOf(Class.class)) {
                //noinspection unchecked
                return EClass.fromJava(userdata.toUserdata(Class.class));
            }
            return EClass.fromJava(userdata.toUserdata().getClass());
        }

        throw new LuaError(new ClassNotFoundException());
    }

}
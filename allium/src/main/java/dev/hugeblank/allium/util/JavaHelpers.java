package dev.hugeblank.allium.util;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.AlliumClassUserdata;
import dev.hugeblank.allium.loader.type.AlliumInstanceUserdata;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;

public class JavaHelpers {


    public static <T> T checkUserdata(LuaValue value, Class<T> clazz) throws LuaError {
        if (value instanceof AlliumInstanceUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clazz);
            } catch (Exception e) {
                throw new LuaError(e);
            }
        } else if (value instanceof AlliumClassUserdata<?> userdata) {
            //noinspection unchecked
            return (T) userdata.toUserdata();
        }
        throw new LuaError("value " + value + " is not an instance of AlliumUserData. Do you have a '.' where a ':' should go?");
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
        } else if (value instanceof LuaTable table && table.rawget("allium_java_class") instanceof AlliumInstanceUserdata<?> userdata) {
            return userdata.toUserdata(EClass.class);
        } else if (value instanceof AlliumClassUserdata<?> userdata) {
            return userdata.toUserdata();
        }

        throw new LuaError(new ClassNotFoundException());
    }

}
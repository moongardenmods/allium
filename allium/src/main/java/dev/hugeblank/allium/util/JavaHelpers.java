package dev.hugeblank.allium.util;

import dev.hugeblank.allium.loader.type.userdata.ClassUserdata;
import dev.hugeblank.allium.loader.type.userdata.InstanceUserdata;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

public class JavaHelpers {


    public static <T> T checkUserdata(LuaValue value, Class<? super T> clazz) throws LuaError {
        if (value instanceof InstanceUserdata<?> userdata) {
            try {
                return userdata.toUserdata(clazz);
            } catch (Exception e) {
                throw new LuaError(e);
            }
        } else if (value instanceof ClassUserdata<?> userdata) {
            //noinspection unchecked
            return (T) userdata.toUserdata();
        }
        throw new LuaError("value " + value + " is not an instance of AlliumUserData. Do you have a '.' where a ':' should go?");
    }

    public static EClass<?> getRawClass(String className) throws LuaError {
            try {
                return EClass.fromJava(Class.forName(className));
            } catch (ClassNotFoundException ignored) {}

            try {
                return EClass.fromJava(Class.forName(className));
            } catch (ClassNotFoundException ignored) {}

        throw new LuaError("Couldn't find class \"" + className + "\"");
    }

    public static EClass<?> asClass(LuaValue value) throws LuaError {
        if (value.isString()) {
            return getRawClass(value.checkString());
        } else if (value.isNil()) {
            return null;
        } else if (value instanceof ClassUserdata<?> userdata) {
            return userdata.toUserdata();
        } else if (value instanceof InstanceUserdata<?> instance) {
            Object out = instance.toUserdata();
            if (out instanceof EClass<?> eClass) {
                return eClass;
            } else if (out instanceof Class<?> clazz) {
                return EClass.fromJava(clazz);
            }
        }

        throw new LuaError(new ClassNotFoundException());
    }

}
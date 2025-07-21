package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.AlliumClassUserdata;
import dev.hugeblank.allium.loader.type.AlliumObjectUserdata;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.NoSuchMappingException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;

public class JavaHelpers {


    public static <T> T checkUserdata(LuaValue value, Class<T> clazz) throws LuaError {
        if (value instanceof AlliumObjectUserdata<?> userdata) {
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
            if (!Allium.DEVELOPMENT)
                className = ScriptRegistry.scriptFromState(state).getMappings()
                        .toUnmappedClassName(Mappings.toSlashedClasspath(className));

            if (className != null) return EClass.fromJava(Class.forName(Mappings.toDottedClasspath(className)));
        } catch (NoSuchMappingException e) {
            // TODO: Warn if not in dev
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
        } else if (value instanceof LuaTable table && table.rawget("allium_java_class") instanceof AlliumObjectUserdata<?> userdata) {
            return userdata.toUserdata(EClass.class);
        } else if (value instanceof AlliumObjectUserdata<?> userdata) {
            if (userdata.instanceOf(EClass.class)) {
                return userdata.toUserdata(EClass.class);
            } else if (userdata.instanceOf(Class.class)) {
                //noinspection unchecked
                return EClass.fromJava(userdata.toUserdata(Class.class));
            }
            return EClass.fromJava(userdata.toUserdata().getClass());
        } else if (value instanceof AlliumClassUserdata userdata) {
            return (EClass<?>) userdata.instance;
        }

        throw new LuaError(new ClassNotFoundException());
    }

}
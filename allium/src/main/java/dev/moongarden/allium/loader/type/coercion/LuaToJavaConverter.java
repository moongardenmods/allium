package dev.moongarden.allium.loader.type.coercion;

import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public interface LuaToJavaConverter<T> {
    T fromLua(LuaState state, LuaValue value) throws LuaError, InvalidArgumentException;
}

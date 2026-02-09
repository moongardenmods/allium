package dev.hugeblank.allium.loader.type.property;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

public class CustomData<T> implements PropertyData<T> {
    private final LuaValue value;

    public CustomData(LuaValue value) {
        this.value = value;
    }

    @Override
    public LuaValue get(String name, LuaState state, T instance, boolean noThisArg) throws LuaError {
        return value;
    }
}

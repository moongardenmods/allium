package dev.moongarden.allium.loader.type.property;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomData<T> implements PropertyData<T> {
    private final Supplier<LuaValue> getter;
    private Setter<T> setter;

    public CustomData(Supplier<LuaValue> getter) {
        this.getter = getter;
    }

    public CustomData(Supplier<LuaValue> getter, Setter<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }


    @Override
    public LuaValue get(String name, LuaState state, T instance, boolean noThisArg) throws LuaError {
        return getter.get();
    }

    @Override
    public void set(String name, LuaState state, T instance, LuaValue value) throws LuaError {
        if (setter == null) PropertyData.super.set(name, state, instance, value);
        setter.set(name, state, instance, value);
    }

    public interface Setter<T> {
        void set(String name, LuaState state, T instance, LuaValue value);
    }
}

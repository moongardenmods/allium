package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.util.Registry;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.util.HashMap;
import java.util.Map;

public class ScriptRegistry extends Registry<Script> {
    public static final ScriptRegistry COMMON;
    public static final ScriptRegistry CLIENT;
    public static final ScriptRegistry DEDICATED;

    private final Map<LuaState, Script> fromState;

    public ScriptRegistry() {
        this.fromState = new HashMap<>();
    }

    public void reloadAll() {
        forEach(Script::reload);
    }

    @Override
    protected void onRegister(Script script) {
        fromState.put(script.getExecutor().getState(), script);
    }

    public void unloadScript(String name) {
        if (super.has(name)) super.get(name).unload();
    }

    public boolean has(LuaState state) {
        return fromState.containsKey(state);
    }

    public Script get(LuaState state) {
        return fromState.get(state);
    }

    public static Script find(LuaState state) throws LuaError {
        if (COMMON.has(state)) return COMMON.get(state);
        if (CLIENT.has(state)) return CLIENT.get(state);
        if (DEDICATED.has(state)) return DEDICATED.get(state);
        throw new LuaError("Unregistered state!");
    }

    public static ScriptRegistry getInstance(Allium.EnvType type) {
        return switch (type) {
            case DEDICATED -> DEDICATED;
            case COMMON -> COMMON;
            case CLIENT -> CLIENT;
        };
    }

    static {
        COMMON = new ScriptRegistry();
        CLIENT = new ScriptRegistry();
        DEDICATED = new ScriptRegistry();
    }

}

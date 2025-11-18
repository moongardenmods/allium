package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.util.Registry;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.util.HashMap;
import java.util.Map;

public class ScriptRegistry extends Registry<Script> {
    public static final ScriptRegistry INSTANCE = new ScriptRegistry();
    private static final Map<LuaState, Script> SCRIPT_STATES = new HashMap<>();

    public ScriptRegistry() {
    }

    public void reloadAll() {
        forEach(Script::reload);
    }

    @Override
    protected void onRegister(Script script) {
        SCRIPT_STATES.put(script.getExecutor().getState(), script);
    }

    public void unloadScript(String name) {
        if (super.has(name)) super.get(name).unload();
    }

    public static Script scriptFromState(LuaState state) throws LuaError {
        if (SCRIPT_STATES.containsKey(state)) return SCRIPT_STATES.get(state);
        throw new LuaError("Unregistered state!");
    }

    public static ScriptRegistry getInstance() {
        return INSTANCE;
    }


}

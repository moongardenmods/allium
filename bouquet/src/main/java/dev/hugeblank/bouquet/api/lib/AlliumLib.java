package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLuaLibrary {
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();

    @LuaWrapped
    public boolean isScriptLoaded(@LuaStateArg LuaState state, String id) throws LuaError {
        return ScriptRegistry.getInstance(ScriptRegistry.find(state).getEnvironment()).has(id);
    }

    @LuaWrapped
    public @CoerceToNative Set<Script> getAllScripts(@LuaStateArg LuaState state) throws LuaError {
        return ScriptRegistry.getInstance(ScriptRegistry.find(state).getEnvironment()).getAll();
    }

    @LuaWrapped
    public @Nullable Script getScript(@LuaStateArg LuaState state, String id) throws LuaError {
        return ScriptRegistry.getInstance(ScriptRegistry.find(state).getEnvironment()).get(id);
    }
}

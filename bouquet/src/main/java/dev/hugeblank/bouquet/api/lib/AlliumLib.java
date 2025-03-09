package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLuaLibrary {
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();

    private final ScriptRegistry.EnvType envType;

    public AlliumLib(Script script, ScriptRegistry.EnvType envType) {
        this.envType = envType;
    }

    @LuaWrapped
    public boolean isScriptLoaded(String id) {
        return ScriptRegistry.getInstance(envType).hasScript(id);
    }

    @LuaWrapped
    public @CoerceToNative Set<Script> getAllScripts() {
        return ScriptRegistry.getInstance(envType).getScripts();
    }

    @LuaWrapped
    public @Nullable Script getScript(String id) {
        return ScriptRegistry.getInstance(envType).getScript(id);
    }
}

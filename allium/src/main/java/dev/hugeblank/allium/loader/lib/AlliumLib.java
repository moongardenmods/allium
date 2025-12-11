package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.api.WrappedLibrary;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLibrary {

    @LuaWrapped
    public boolean isScriptLoaded(String id) {
        return ScriptRegistry.getInstance().has(id);
    }

    @LuaWrapped
    public @CoerceToNative Set<Script> getAllScripts() {
        return ScriptRegistry.getInstance().getAll();
    }

    @LuaWrapped
    public @Nullable Script getScript(String id) {
        return ScriptRegistry.getInstance().get(id);
    }

}

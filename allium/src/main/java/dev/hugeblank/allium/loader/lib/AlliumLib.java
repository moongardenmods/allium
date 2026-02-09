package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLibrary {

    @LuaWrapped
    public static String environment() {
        return switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> "client";
            case SERVER -> "server";
        };
    }

    @LuaWrapped
    public static boolean isScriptPresent(String id) {
        return ScriptRegistry.getInstance().has(id) &&
                ScriptRegistry.getInstance().get(id).getLaunchState().equals(Script.State.INITIALIZED);
    }

    @LuaWrapped
    public static @CoerceToNative List<Script> getAllScripts() {
        return ScriptRegistry.getInstance().getAll().stream()
                .filter((script) -> script.getLaunchState().equals(Script.State.INITIALIZED)).toList();
    }

    @LuaWrapped
    public static @Nullable Script getScript(String id) {
        Script instance = ScriptRegistry.getInstance().get(id);
        if (instance != null && instance.getLaunchState().equals(Script.State.INITIALIZED)) {
            return ScriptRegistry.getInstance().get(id);
        }
        return null;
    }

}

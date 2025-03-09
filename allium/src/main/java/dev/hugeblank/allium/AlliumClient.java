package dev.hugeblank.allium;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;

public class AlliumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SetupHelpers.initializeExtensions(EnvType.CLIENT);
        SetupHelpers.collectScripts(ScriptRegistry.EnvType.CLIENT);
        SetupHelpers.initializeScripts(ScriptRegistry.EnvType.CLIENT);
    }
}

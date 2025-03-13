package dev.hugeblank.allium;

import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.ClientModInitializer;

public class AlliumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SetupHelpers.initializeEnvironment(Allium.EnvType.CLIENT);
    }
}

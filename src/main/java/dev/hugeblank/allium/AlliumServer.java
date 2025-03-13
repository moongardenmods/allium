package dev.hugeblank.allium;

import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.DedicatedServerModInitializer;

public class AlliumServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        SetupHelpers.initializeEnvironment(Allium.EnvType.DEDICATED);
    }
}

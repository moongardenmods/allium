package dev.hugeblank.allium.fabric;

import dev.hugeblank.allium.Allium;
import net.fabricmc.api.DedicatedServerModInitializer;

public class AlliumServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        Allium.initServer();
    }
}

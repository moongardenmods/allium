package dev.hugeblank.allium.fabric;

import dev.hugeblank.allium.Allium;
import net.fabricmc.api.ClientModInitializer;

public class AlliumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Allium.initClient();
    }
}

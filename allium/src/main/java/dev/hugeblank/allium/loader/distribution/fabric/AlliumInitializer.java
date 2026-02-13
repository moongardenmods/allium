package dev.hugeblank.allium.loader.distribution.fabric;

import net.fabricmc.api.ModInitializer;

import static dev.hugeblank.allium.Allium.PROFILER;

public class AlliumInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        PROFILER.print();
    }
}

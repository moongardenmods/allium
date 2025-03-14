package dev.hugeblank.allium.fabric;

import dev.hugeblank.allium.Allium;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AlliumPrelaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        Allium.preLaunch();
    }
}

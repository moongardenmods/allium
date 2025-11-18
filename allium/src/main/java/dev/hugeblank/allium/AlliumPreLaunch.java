package dev.hugeblank.allium;

import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AlliumPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {

        SetupHelpers.initializeDirectories();

        SetupHelpers.collectScripts();

        MixinConfigUtil.applyConfiguration();
    }

}

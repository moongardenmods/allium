package dev.hugeblank.allium;

import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.YarnLoader;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AlliumPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {

        SetupHelpers.initializeDirectories();

        Mappings.LOADERS.register(new YarnLoader());

        EnvType envType = FabricLoader.getInstance().getEnvironmentType();

        SetupHelpers.collectScripts();

        MixinConfigUtil.applyConfiguration();
    }

}

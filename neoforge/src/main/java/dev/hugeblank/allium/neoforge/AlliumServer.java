package dev.hugeblank.allium.neoforge;

import dev.hugeblank.allium.Allium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Allium.ID, dist = Dist.DEDICATED_SERVER)
public class AlliumServer {
    public AlliumServer() {
        Allium.initServer();
    }
}

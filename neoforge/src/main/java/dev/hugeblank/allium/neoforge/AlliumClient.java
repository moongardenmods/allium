package dev.hugeblank.allium.neoforge;

import dev.hugeblank.allium.Allium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Allium.ID, dist = Dist.CLIENT)
public class AlliumClient {
    public AlliumClient() {
        Allium.initClient();
    }
}

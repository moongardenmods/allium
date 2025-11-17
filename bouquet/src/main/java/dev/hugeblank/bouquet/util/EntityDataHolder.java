package dev.hugeblank.bouquet.util;

import net.minecraft.world.entity.Entity;
import org.squiddev.cobalt.LuaValue;

public interface EntityDataHolder {
    LuaValue allium$getTemporaryData(String key);
    void allium$setTemporaryData(String key, LuaValue value);

    void allium_private$copyTempData(Entity source);
}

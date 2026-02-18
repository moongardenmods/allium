-- Let's add an allium on top of fully grown hand-planted cacti!
mixin.to("net.minecraft.world.level.block.CactusBlock") -- Mix into the block class
    :createInjectMethod("add_flower", {
        mixin.annotation.inject({ -- Target the randomTick method, at a specific point after the cacti has grown
            method = { "randomTick" }, -- If there's only one method in the target class with a given name, we do not have to provide a descriptor. Nifty!
            at = { {
                       value = "INVOKE",
                       target = "Lnet/minecraft/server/level/ServerLevel;neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V"
                   } }
        })
    }, { -- Grab some of the surrounding variables using localrefs (MixinExtra's @Local annotation).
        mixin.sugar.localref("Lnet/minecraft/core/BlockPos;", {ordinal = 0}), -- Get this block pos, not necessary but it's nice to demonstrate that we can
        mixin.sugar.localref("I", {ordinal = 0}) -- Get the cactus growth level.
    })
    :build()
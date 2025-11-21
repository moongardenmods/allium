-- Let's add an allium on top of fully grown hand-planted cacti!
local CactusBlockMixin = mixin.to("net.minecraft.world.level.block.CactusBlock") -- Mix into the block class
CactusBlockMixin:inject("add_flower", { -- Target the randomTick method, at a specific point after the cacti has grown
    method = { "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V" },
    at = { {
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;Z)V"
        } }
}, {
    mixin.getLocal("Lnet/minecraft/core/BlockPos;", {ordinal = 0}), -- Get this block pos, not necessary but it's nice to demonstrate that we can
    mixin.getLocal("I", {ordinal = 0}) -- Get the cactus growth level.
})

CactusBlockMixin:build()

local PlayerMixin = mixin.to("net.minecraft.world.entity.player.Player")
PlayerMixin:modifyArgs("skip_fall_player", {
    method = { "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z" },
    at = {
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Avatar;causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z"
    }
})

PlayerMixin:build()
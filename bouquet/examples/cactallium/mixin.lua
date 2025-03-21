-- Let's add an allium on top of fully grown hand-planted cacti!
local CactusBlockMixin = mixin.to("net.minecraft.block.CactusBlock") -- Mix into the block class
addFlower = CactusBlockMixin:inject("add_flower", { -- Target the randomTick method, at a specific point after the cacti has grown
    method = { "randomTick(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)V" },
    at = { {
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;updateNeighbor(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Z)V"
        } }
}, {
    mixin.getLocal("Lnet/minecraft/util/math/BlockPos;", "ordinal", 0), -- Get this block pos, not necessary but it's nice to demonstrate that we can
    mixin.getLocal("I", "ordinal", 0) -- Get the cactus growth level.
})

CactusBlockMixin:build()
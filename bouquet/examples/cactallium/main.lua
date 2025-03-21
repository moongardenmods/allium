-- Cactallium
-- By hugeblank - March 20, 2025
-- Start by heading over to mixin.lua!

local Blocks = require("net.minecraft.block.Blocks")

addFlower:register(script, function(self, state, world, pos, random, ci, blockPos, i)
    -- If the cactus is currently 2 blocks tall (not including the block that was just added prior to this function
    -- being called...)
    if i == 2 then
        -- Add the flower. How cute!
        world:setBlockState(blockPos:up():up(), Blocks.ALLIUM:getDefaultState())
    end
end)
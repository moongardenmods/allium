-- Cactallium
-- By hugeblank - March 20, 2025
-- Start by heading over to mixin.lua!

local Blocks = require("net.minecraft.block.Blocks")

addFlower:register(script, function(self, state, world, pos, random, ci, blockPos, i)
    -- If the cactus is 3 blocks tall, add an allium above
    if i == 3 then
        -- Add the flower. How cute!
        world:setBlockState(blockPos:up(), Blocks.ALLIUM:getDefaultState())
    end
end)
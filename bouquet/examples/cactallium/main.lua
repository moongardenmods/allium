-- Cactallium
-- By hugeblank - March 20, 2025
-- Start by heading over to mixin.lua!

local Blocks = require("net.minecraft.world.level.block.Blocks")

local addFlower = mixin.get(script, "add_flower")

-- Note that because this mixin doesn't do anything to any registries, it's dynamic! We could, say, change the flower
-- to a poppy! Or to a diamond block!

addFlower:register(script, function(self, state, world, pos, random, ci, blockPos, i)
    -- If the cactus is currently 2 blocks tall (not including the block that was just added prior to this function
    -- being called...)
    if i == 2 then
        -- Add the flower. How cute!
        world:setBlockAndUpdate(blockPos:above():above(), Blocks.ALLIUM:defaultBlockState())
    end
end)
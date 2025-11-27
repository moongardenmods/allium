-- Luckbrew
-- By hugeblank - April 8, 2023
-- Mixins are just like events under the hood! This means that depending on where you're mixing into, it could be
-- beneficial to put it in a dynamic entrypoint, to enable easy modification and reloading!
-- Unfortunately here, the brewing recipe registry gets created only once on game start, so this file is not dynamic.

-- Get the event for the mixin we created in mixin.lua
local addRecipes = mixin.get("add_brewing_recipes")

addRecipes:hook(function(builder, ci)
    local Items = require("net.minecraft.world.item.Items")
    local Potions = require("net.minecraft.world.item.alchemy.Potions")
    print("registering lucky potion!")
    -- Register our lucky little potion
    builder:addMix(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK)
end)
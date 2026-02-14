-- Luckbrew
-- By hugeblank - April 8, 2023
-- Mixins are just like events under the hood! This means that depending on where you're mixing into, it could be
-- beneficial to put it in script:registerReloadable(), to enable easy modification and reloading!
-- Unfortunately here, the brewing recipe registry gets created only once on game start, so this mixin is not reloadable.

-- Get the event for the mixin we created in mixin.lua using the unique name.
local addRecipes = mixin.get("add_brewing_recipes")

local Items = require("net.minecraft.world.item.Items")
local Potions = require("net.minecraft.world.item.alchemy.Potions")

-- Create the hook, filling in the body of the method we made.
addRecipes:hook(function(builder, ci)
    print("registering lucky potion!")
    -- Register our lucky little potion
    builder:addMix(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK)
end)
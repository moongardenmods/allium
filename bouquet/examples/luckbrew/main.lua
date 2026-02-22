-- Luckbrew
-- By hugeblank - April 8, 2023
-- Mixins are just like events under the hood! This means that depending on where you're mixing into, it could be
-- beneficial to put it in script:registerReloadable(), to enable easy modification and reloading!
-- Unfortunately here, the brewing recipe registry gets created only once on game start, so this mixin is not reloadable.

-- Get the event for the mixin we created in mixin.lua using the unique name.

local Items = require("net.minecraft.world.item.Items")
local Potions = require("net.minecraft.world.item.alchemy.Potions")

local definition = {}

function definition:addBrewingRecipes(ci)
    print("registering lucky potion!")
    -- Register our lucky little potion
    self:addMix(Potions.AWKWARD, Items.GOLD_NUGGET, Potions.LUCK)
end

mixin.get("potion_brewing_mixin"):define(definition)
-- Create the hook, filling in the body of the method we made.

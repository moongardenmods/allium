-- Wetworks
-- By hugeblank - March 22, 2022
-- Applies the 1.19 mud water bottle mechanic to concrete powder blocks
-- This file registers the events in a function passed to script:registerReloadable().
-- Try modifying, and running /reload!

-- Import java classes
local Items = require("net.minecraft.world.item.Items")
local BuiltInRegistries = require("net.minecraft.core.registries.BuiltInRegistries")
local Identifier = require("net.minecraft.resources.Identifier")
local SoundEvents = require("net.minecraft.sounds.SoundEvents")
local SoundSource = require("net.minecraft.sounds.SoundSource")
local EquipmentSlot = require("net.minecraft.world.entity.EquipmentSlot")

local DataComponents = require("net.minecraft.core.component.DataComponents")
local PotionContents = require("net.minecraft.world.item.alchemy.PotionContents")
local Potions = require("net.minecraft.world.item.alchemy.Potions")
local ParticleTypes = require("net.minecraft.core.particles.ParticleTypes")

local events = require("bouquet").events

script:registerReloadable(function()
    -- Return a function that we can modify while the game is playing
    events.common.blockInteract:register(script, function(state, level, pos, player, hand, hitResult)
        local concrete = BuiltInRegistries.BLOCK:getKey(state:getBlock()):getPath() -- Get the name of the block interacted with
        local mainHandStack = player:getItemBySlot(EquipmentSlot.MAINHAND) -- Get the main hand itemstack of the player
        local stackComponents = mainHandStack:getComponents()
        -- Check if the block name has 'concrete_powder' in it, then check if the main hand is holding a water bottle
        if concrete:find("concrete_powder") and
                stackComponents:getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY):is(Potions.WATER) then
            if not level:isClientSide() then
                -- Spawn some particles
                level:sendParticles(ParticleTypes.SPLASH,
                        pos:getX() + level.random:nextDouble(),
                        pos:getY() + level.random:nextDouble(),
                        pos:getZ() + level.random:nextDouble(),
                        1, 0, 0, 0, 1
                )
            end

            -- Replace the powder block with the concrete variant
            level:setBlockAndUpdate(pos, BuiltInRegistries.BLOCK:getValue(Identifier.parse("minecraft:"..concrete:gsub("_powder", ""))):defaultBlockState())

            -- Play sound effects
            level:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1, 1)
            level:playSound(nil, pos:getX(), pos:getY(), pos:getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1, 1)
            if (not player:isCreative()) then -- If the player isn't in creative
                mainHandStack:setCount(0) -- Remove the water bottle
                player:setItemSlot(EquipmentSlot.MAINHAND, Items.GLASS_BOTTLE:getDefaultInstance()) -- Replace it with an empty glass bottle
            end
        end
    end)
end)
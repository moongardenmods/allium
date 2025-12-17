-- Moonflower
-- By hugeblank - Dec 10, 2025

local FlowerBlock = require("net.minecraft.world.level.block.FlowerBlock")
local BlockState = require("net.minecraft.world.level.block.state.BlockState")
local Level = require("net.minecraft.world.level.Level")
local BlockPos = require("net.minecraft.core.BlockPos")
local Boolean = require("java.lang.Boolean")
local Identifier = require("net.minecraft.resources.Identifier")
local ResourceKey = require("net.minecraft.resources.ResourceKey")
local Registries = require("net.minecraft.core.registries.Registries")
local Blocks = require("net.minecraft.world.level.block.Blocks")
local Items = require("net.minecraft.world.item.Items")
local MobEffects = require("net.minecraft.world.effect.MobEffects")
local Properties = require("net.minecraft.world.level.block.state.BlockBehaviour$Properties")
local MapColor = require("net.minecraft.world.level.material.MapColor")
local SoundType = require("net.minecraft.world.level.block.SoundType")
local OffsetType = require("net.minecraft.world.level.block.state.BlockBehaviour$OffsetType")
local PushReaction = require("net.minecraft.world.level.material.PushReaction")

local function register(id, blockInitializer, settings)
    local identifier = Identifier.fromNamespaceAndPath(script:getID(), id)
    local key = ResourceKey.create(Registries.BLOCK, identifier)
    local block = Blocks.register(key, blockInitializer, settings)
    Items.registerBlock(block)
    return block
end

local builder = java.extendClass(FlowerBlock)

builder:overrideMethod("onPlace", {BlockState, Level, BlockPos, BlockState, Boolean}, {}, function(this, state, level, pos, oldState, movedByPiston)

end)

local MoonFlowerBlock = builder:build()

--local moonflower = register("moonflower", function(p)
--    return MoonFlowerBlock(MobEffects.NIGHT_VISION, 8.0, p)
--end, Properties.of():mapColor(MapColor.DIAMOND):noCollision():instabreak():sound(SoundType.GRASS):offsetType(OffsetType.XZ):pushReaction(PushReaction.DESTROY))
--
--if allium.environment() == "client" then
--    local BlockRenderLayerMap = require("net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap")
--    local ChunkSectionLayer = require("net.minecraft.client.renderer.chunk.ChunkSectionLayer")
--    BlockRenderLayerMap.putBlock(moonflower, ChunkSectionLayer.CUTOUT)
--end
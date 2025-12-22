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
local MobEffects = require("net.minecraft.world.effect.MobEffects")
local BlockBehaviourProperties = require("net.minecraft.world.level.block.state.BlockBehaviour$Properties")
local MapColor = require("net.minecraft.world.level.material.MapColor")
local SoundType = require("net.minecraft.world.level.block.SoundType")
local OffsetType = require("net.minecraft.world.level.block.state.BlockBehaviour$OffsetType")
local PushReaction = require("net.minecraft.world.level.material.PushReaction")
local Item = require("net.minecraft.world.item.Item")
local BlockItem = require("net.minecraft.world.item.BlockItem")
local ItemProperties = require("net.minecraft.world.item.Item$Properties")
local Registry = require("net.minecraft.core.Registry")
local BuiltinRegistries = require("net.minecraft.core.registries.BuiltInRegistries")

local function registerBlockItem(block)
    local key = ResourceKey.create(Registries.ITEM, block:builtInRegistryHolder():key():identifier())
    local blockItem = BlockItem(block, ItemProperties():useBlockDescriptionPrefix():setId(key))
    blockItem:registerBlocks(Item.BY_BLOCK, blockItem)
    return Registry.register(BuiltinRegistries.ITEM, key, blockItem)
end

local function registerBlock(id, blockInitializer, settings)
    local identifier = Identifier.fromNamespaceAndPath(script:getID(), id)
    local key = ResourceKey.create(Registries.BLOCK, identifier)
    local block = Registry.register(BuiltinRegistries.BLOCK, key, blockInitializer(settings:setId(key)))
    registerBlockItem(block)
    return block
end

local builder = java.extendClass(FlowerBlock)

builder:override("onPlace", {BlockState, Level, BlockPos, BlockState, Boolean}, {})
function builder:onPlace(this, state, level, pos, oldState, movedByPiston)
    --print(tostring(this), tostring(state), tostring(level), tostring(pos), tostring(oldState), movedByPiston)
end

local MoonFlowerBlock = builder:build()

local moonflower = registerBlock("moonflower", function(p)
    return MoonFlowerBlock(MobEffects.NIGHT_VISION, 8.0, p)
end, BlockBehaviourProperties.of():mapColor(MapColor.DIAMOND):noCollision():instabreak():sound(SoundType.GRASS):offsetType(OffsetType.XZ):pushReaction(PushReaction.DESTROY))

if allium.environment() == "client" then
    local BlockRenderLayerMap = require("net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap")
    local ChunkSectionLayer = require("net.minecraft.client.renderer.chunk.ChunkSectionLayer")
    BlockRenderLayerMap.putBlock(moonflower, ChunkSectionLayer.CUTOUT)
end
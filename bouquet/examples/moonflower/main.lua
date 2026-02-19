-- Moonflower
-- By hugeblank - Dec 10, 2025

local FlowerBlock = require("net.minecraft.world.level.block.FlowerBlock")
local BlockState = require("net.minecraft.world.level.block.state.BlockState")
local Level = require("net.minecraft.world.level.Level")
local BlockPos = require("net.minecraft.core.BlockPos")
local Holder = require("net.minecraft.core.Holder")
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

local definition = {}

function definition:constructor(holder, float, behavior)
    print(holder, float, behavior)
    self.meaningOfLife = 42
end

function definition:onPlace(state, level, pos, oldState, movedByPiston)
    print(self.meaningOfLife)
end

-- Note: If defining a constructor that matches one in the parent class, a constructor function definition is optional.
local MoonFlowerBlock = java.extendClass(FlowerBlock)
    :field("meaningOfLife", java.int, { final = true }, nil)
    :constructor()
        :super({Holder, java.float, BlockBehaviourProperties})
        :access({ public = true })
        :definesFields()
        :build()
    :method()
        :access({ public = true })
        :override("onPlace", {BlockState, Level, BlockPos, BlockState, java.boolean})
        :build()
    :define(definition)
    :build()

local moonflower = registerBlock(
    "moonflower",
    function(p)
        return MoonFlowerBlock(MobEffects.NIGHT_VISION, 8.0, p)
    end,
    BlockBehaviourProperties.of()
        :mapColor(MapColor.DIAMOND)
        :noCollision():instabreak()
        :sound(SoundType.GRASS)
        :offsetType(OffsetType.XZ)
        :pushReaction(PushReaction.DESTROY)
)

if allium.environment() == "client" then
    local ChunkSectionLayerMap = require("net.fabricmc.fabric.api.client.rendering.v1.ChunkSectionLayerMap")
    local ChunkSectionLayer = require("net.minecraft.client.renderer.chunk.ChunkSectionLayer")
    ChunkSectionLayerMap.putBlock(moonflower, ChunkSectionLayer.CUTOUT)
end
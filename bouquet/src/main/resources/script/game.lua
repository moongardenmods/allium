local game = {}

local BuiltInRegistries = require("net.minecraft.core.registries.BuiltInRegistries")
local Identifier = require("net.minecraft.resources.Identifier")
local ResourceKey = require("net.minecraft.resources.ResourceKey")
local Registries = require("net.minecraft.core.registries.Registries")
local Collectors = require("java.util.stream.Collectors")
local StreamSupport = require("java.util.stream.StreamSupport")
local MinecraftServer = require("net.minecraft.server.MinecraftServer")
local Map = require("java.util.Map")
local String = require("java.lang.String")
local Block = require("net.minecraft.world.level.block.Block")
local ServerLevel = require("net.minecraft.server.level.ServerLevel")

local function assertType(i, input, class)
    assert(java.instanceOf(input, class), "Argumemnt #"..i..": Expected type "..class:name()..", got ".. type(input))
end

local function unwrapRef(optRef)
    return optRef:value()
end

game.getBlock = function(id)
    assertType(1, id, String)
    local ref = BuiltInRegistries.BLOCK:get(Identifier.parse(id))
    return ref:map(unwrapRef):orElse(nil)
end

game.getItem = function(id)
    assertType(1, id, String)
    local ref = BuiltInRegistries.ITEM:get(Identifier.parse(id))
    return ref:map(unwrapRef):orElse(nil)
end

game.getLevel = function(server, id)
    assertType(1, server, MinecraftServer)
    assertType(2, id, String)
    return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(id)))
end

---@return table<string, userdata>
game.listBlocks = function()
    return java.coerce(BuiltInRegistries.BLOCK:stream():collect(Collectors.toMap(function(x)
                return BuiltInRegistries.BLOCK:getKey(x):toString()
            end, function(x) return x end)), Map[{String, Block}])
end

game.listLevels = function(server)
    assertType(1, server, MinecraftServer)
    return java.coerce(
            StreamSupport.stream(server:getAllLevels():spliterator(), false):collect(Collectors.toMap(function(x)
                x:dimension():identifier()
            end, function(x) return x end)),
            Map[{Identifier, ServerLevel}]
    )
end

return game
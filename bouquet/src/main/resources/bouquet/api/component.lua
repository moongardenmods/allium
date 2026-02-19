local Script = require("dev.moongarden.allium.loader.Script")
local ComponentSerialization = require("net.minecraft.network.chat.ComponentSerialization")
local LuaOps = require("dev.moongarden.bouquet.util.LuaOps")
local LuaValue = require("org.squiddev.cobalt.LuaValue")
local Component = require("net.minecraft.network.chat.Component")

local util = require("bouquet.util")

local function testResult(result)
    if result:isError() and result:error():isPresent() then
        error(result:error():get():message())
    elseif result:isError() then
        error("An unknown error occurred during parsing.")
    end
    return result:getOrThrow()
end

-- Parses components to and from a table.
-- For information on how to format a component table, see: https://minecraft.wiki/w/Text_component_format#Content_types

return function(script)
    util.assertType(1, script, Script)
    local ops = LuaOps(script:getState())
    return {
        fromTable = function(data)
            assert(type(data) == "table", "Argument #1: Expected type table, got "..type(data))
            return testResult(java.callWith(ComponentSerialization.CODEC.parse, {LuaOps, LuaValue}, ComponentSerialization.CODEC, ops, data))
        end,
        toTable = function(component)
            util.assertType(1, component, Component)
            return testResult(java.callWith(ComponentSerialization.CODEC.encodeStart, {LuaOps, Component}, ComponentSerialization.CODEC, ops, component))
        end
    }
end
local Script = require("dev.hugeblank.allium.loader.Script")
local LiteralArgumentBuilder = require("com.mojang.brigadier.builder.LiteralArgumentBuilder")
local CommandSourceStack = require("net.minecraft.commands.CommandSourceStack")
local CommandSelection = require("net.minecraft.commands.Commands$CommandSelection")
local StaticBinder = require("dev.hugeblank.allium.loader.type.StaticBinder")
local EClass = require("me.basiqueevangelist.enhancedreflection.api.EClass")

local util = require("bouquet.util")

return {
    register = function(script, builder, environment)
        util.assertType(1, script, Script)
        util.assertType(2, builder, LiteralArgumentBuilder[{CommandSourceStack}])
        if environment ~= nil then util.assertType(3, environment, CommandSelection) end
        table.insert(util.holders.commands, {
            script = script,
            builder = builder,
            environment = environment or CommandSelection.ALL
        })
    end,
    arguments = setmetatable(util.holders.argumentTypes, {
        __index = function(t, key)
            local val = rawget(t, key)
            if not val then
                val = rawget(t, "brigadier:"..key)
            end
            if val then
                return StaticBinder.bindClass(EClass.fromJava(val))
            end
        end
    })
}
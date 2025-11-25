local Script = require("dev.hugeblank.allium.loader.Script")
local LiteralArgumentBuilder = require("com.mojang.brigadier.builder.LiteralArgumentBuilder")
local CommandSourceStack = require("net.minecraft.commands.CommandSourceStack")
local CommandSelection = require("net.minecraft.commands.Commands$CommandSelection")
local CommandRegisterEntry = require("dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry")

local util = require("script.util")

return {
    register = function(script, builder, environment)
        util.assertType(1, script, Script)
        util.assertType(2, builder, LiteralArgumentBuilder[{CommandSourceStack}])
        if environment == nil then util.assertType(3, environment, CommandSelection) end
        table.insert(util.holders.commands, CommandRegisterEntry(
                script,
                builder,
                environment or CommandSelection.ALL
        ))
    end,
    arguments = util.holders.argumentTypes
}
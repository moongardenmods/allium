local CommandSelection = require("net.minecraft.commands.Commands$CommandSelection")

local events = require("bouquet.api.events")

local util = require("bouquet.util")
--local command = require("build.bouquet.resources.main.bouquet.api.command")

local argtypedef = {}

function argtypedef:registerArgumentTypeInfos(id, brigadierType, info, cir)
    util.holders.argumentTypes[id] = brigadierType
end

mixin.get("argument_type_infos_mixin"):define(argtypedef)

local function queueRegisterEvent(entry, result)
    events.server.commandRegister:invoker():onCommandRegistration(
            entry.script:getID(),
            entry.builder:getLiteral(),
            result
    )
end

local commandsdef = {}

function commandsdef:initCommands(commandSelection, context, ci)
    for _, entry in ipairs(util.holders.commands) do
        if (
            (commandSelection:equals(entry.environment) or entry.environment:equals(CommandSelection.ALL)) and
            self:getDispatcher():getRoot():getChild(entry.builder:getLiteral()) == nil
        ) then
            self:getDispatcher():register(entry.builder)
            queueRegisterEvent(entry, true)
        else
            queueRegisterEvent(entry, false)
        end
    end
end

mixin.get("commands_mixin"):define(commandsdef)
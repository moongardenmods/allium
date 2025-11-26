local ServerEvents = require("dev.hugeblank.bouquet.api.event.ServerEvents")

local util = require("script.util")

mixin.get("argument_type_infos_register"):hook(function(registry, id, brigadierType, info, cir)
    util.holders.argumentTypes[id] = brigadierType
end)

local function queueRegisterEvent(entry, result)
    ServerEvents.COMMAND_REGISTER:invoker():onCommandRegistration(
            entry.script:getID(),
            entry.builder:getLiteral(),
            result
    )
end

mixin.get("commands_init"):hook(function(self, commandSelection, context, ci)
    for _, entry in ipairs(util.holders.commands) do
        if (
                (commandSelection:equals(entry.environment) or entry.environment:equals(CommandSelection.ALL)) and
                        self:getDispatcher():getRoot():getChild(entry.builder:getLiteral()) == nil
        ) then
            self:getDispatcher():register(entry.builder)
            queueRegisterEvent(entry, true)
            return
        end
        queueRegisterEvent(entry, false)
    end
end)
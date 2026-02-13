-- What's my IP?
-- By BasiqueEvangelist - Jan 02, 2026
-- A simple script to demonstrate the HTTP API in Bouquet.
-- Registers /whats_my_ip, which shows your public IP address when run.

local bouquet = require("bouquet")
local Commands = require("net.minecraft.commands.Commands")
local Component = require("net.minecraft.network.chat.Component")

script:registerReloadable(function()
    bouquet.command.register(script, Commands.literal("whats_my_ip"):executes(function(context)
        local req = bouquet.http:request("https://httpbin.org/get")
        req:send():thenAccept(function(res)
            local json = res.body:asJson()
            print(json)
            context:getSource():sendSuccess(function()
                return Component.literal(json.origin)
            end, false)
        end)

        return 0
    end))
end)
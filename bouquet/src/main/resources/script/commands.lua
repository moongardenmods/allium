
local MinecraftServer
if package.environment() == "server" then
    MinecraftServer = require("net.minecraft.server.MinecraftServer")
end

local util = require("script.util")

return {
    exec = function(server, ...)
        util.assertServer()
        local parameters = table.pack(...)
        util.assertType(1, server, MinecraftServer)
        for i, v in ipairs(parameters) do
            assert(type(v) == "string", "Argument #"..(i+1)..": Expected type 'string', got ".. type(v))
        end
        server:getCommands():performPrefixedCommand(
                server:createCommandSourceStack(),
                table.concat(parameters, " ")
        )
    end

}
require("script.hooks")
print("Hello from Bouquet's Script!")
local command = require("script.command")
for k, v in pairs(command.arguments) do
    print(k, v)
end
return {
    game = require("script.game"),
    fabric = require("script.fabric"),
    commands = require("script.commands"),
    command = require("script.command")
}
require("script.hooks")
print("Hello from Bouquet's Script!")
local command = require("script.command")
for k, v in pairs(command.arguments) do
    print(k, v)
end
return {
    command = require("script.command"),
    commands = require("script.commands"),
    fabric = require("script.fabric"),
    game = require("script.game"),
    json = require("script.json"),
    tag = require("script.tag"),
}
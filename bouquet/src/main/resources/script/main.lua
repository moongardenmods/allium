print("Hello from Bouquet")
local game = require("script.game")
local list = game.listBlocks()
for k, v in pairs(list) do
    print(k, "-", v:getClass():getName())
end
require("bouquet.hooks")

local FsLib = require("dev.hugeblank.bouquet.api.lib.fs.FsLib")
local HttpLib = require("dev.hugeblank.bouquet.api.lib.http.HttpLib")
local RecipeLib = require("dev.hugeblank.bouquet.api.lib.recipe.RecipeLib")

local function wrapAs(class)
    return function(script)
        local lib = class(script)
        local out = {}
        for k, v in pairs(lib) do
            out[k] = function(...)
                return v(lib, ...)
            end
        end
        return out
    end
end

print("Hello from Bouquet's Script! Environment:", allium.environment())
return {
    command = require("bouquet.api.command"),
    commands = require("bouquet.api.commands"),
    component = require("bouquet.api.component"),
    config = require("bouquet.api.config"),
    events = require("bouquet.api.events"),
    fabric = require("bouquet.api.fabric"),
    fs = wrapAs(FsLib),
    game = require("bouquet.api.game"),
    http = HttpLib.INSTANCE,
    json = require("bouquet.api.json"),
    recipe = RecipeLib.INSTANCE,
    tag = require("bouquet.api.tag"),
}
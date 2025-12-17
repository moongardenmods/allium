require("bouquet.hooks")

local FsLib = require("dev.hugeblank.bouquet.api.lib.fs.FsLib")
local HttpLib = require("dev.hugeblank.bouquet.api.lib.http.HttpLib")

print("Hello from Bouquet's Script! Environment:", allium.environment())
local request = HttpLib().request
return {
    command = require("bouquet.api.command"),
    commands = require("bouquet.api.commands"),
    config = require("bouquet.api.config"),
    events = require("bouquet.api.events"),
    fabric = require("bouquet.api.fabric"),
    fs = {
        as = function(script)
            local ofs = FsLib(script)
            local fs = {}
            for k, v in pairs(ofs) do
                fs[k] = function(...)
                    return v(ofs, ...)
                end
            end
            return fs
        end
    },
    game = require("bouquet.api.game"),
    http = {
        request = request
    },
    json = require("bouquet.api.json"),
    tag = require("bouquet.api.tag"),
}
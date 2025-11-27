local FabricLoader = require("net.fabricmc.loader.api.FabricLoader")
local ModContainer = require("net.fabricmc.loader.api.ModContainer")

local String = require("java.lang.String")
local List = require("java.util.List")

local assertType = require("bouquet.util").assertType

local loader = FabricLoader.getInstance()

return {
    getAllMods = function()
        return java.coerce(loader:getAllMods():stream():toList(), List[{ ModContainer}])
    end,

    getMod = function(id)
        assertType(1, id, String)
        return loader:getModContainer(id):orElse(nil)
    end,

    isModLoaded = function(id)
        return loader:getModContainer(id)
    end
}
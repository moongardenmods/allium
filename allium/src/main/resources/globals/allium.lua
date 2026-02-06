---@meta

---@class Allium
allium = {}

--- Get the current environment the game is running in. If `"client"`, then client specific rendering classes are available for use. If `"server"`, then the client logic is unavailable, but anything else is available.
--- @return "client"|"server" environment A class builder.
function allium.environment() end

--- Check if a script with the given ID is present and initialized.
--- @param id string The script ID to check for.
--- @return boolean present Whether or not a script with the given ID is present.
function allium.isScriptPresent(id) end

--- Get all currently running scripts.
--- @return table<Script> scripts A table of scripts.
function allium.getAllScripts() end

--- Get a script with the given ID.
--- @param id string The script ID to get.
--- @return Script script A script, if one with the given ID exists.
function allium.getScript(id) end
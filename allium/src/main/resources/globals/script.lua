---@meta

---@class Script
script = {}

--- Returns the script ID provided in the manifest.
---@return string
function script:getID() end

--- Returns the script version provided in the manifest.
---@return string
function script:getVersion() end

--- Returns the detailed script name provided in the manifest.
---@return string
function script:getName() end

--- Returns the scripts module. Functionally identical to `require`ing the script.
--- @return any
function script:getModule() end

--- Returns the scripts internal Lua state. Be careful, this exposes the script's globals.
--- @return InstanceUserdata
function script:getState() end

--- Register a resource that can be closed at will, OR when the script gets unloaded or reloaded.
--- @param onClose function Function to be called when the resource is closed.
--- @return ResourceRegistration
function script:registerResource(onClose) end
local FileHelper = require("dev.moongarden.allium.util.FileHelper")
local Files = require("java.nio.file.Files")
local StandardCharsets = require("java.nio.charset.StandardCharsets")

local json = require("bouquet.api.json")

return {
    as = function(script)
        local path = FileHelper.CONFIG_DIR:resolve(script:getID()..".json")
        return {
            load = function()
                if Files.exists(path) then
                    return json.fromJson(Files.readString(path))
                end
                return nil
            end,
            save = function(data)
                local cfg = json.pretty:toJson(json.toJsonElement(data))
                Files.deleteIfExists(path)
                local outputStream = Files.newOutputStream(path)
                outputStream:write(cfg.getBytes(StandardCharsets.UTF_8))
                outputStream:close()
            end
        }
    end
}
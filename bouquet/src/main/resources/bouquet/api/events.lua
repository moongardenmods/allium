local CommonEvents = require("dev.moongarden.bouquet.api.event.CommonEvents")
local ServerEvents = require("dev.moongarden.bouquet.api.event.ServerEvents")

local out = {
    common = {
        playerTick = CommonEvents.PLAYER_TICK,
        playerDeath = CommonEvents.PLAYER_DEATH,
        blockInteract = CommonEvents.BLOCK_INTERACT,
        entitySave = CommonEvents.ENTITY_SAVE,
        entityLoad = CommonEvents.ENTITY_LOAD,
    },
    server = {
        chatMessage = ServerEvents.CHAT_MESSAGE,
        playerJoin = ServerEvents.PLAYER_JOIN,
        playerQuit = ServerEvents.PLAYER_QUIT,
        playerBlockCollision = ServerEvents.PLAYER_BLOCK_COLLISION,
        serverStart = ServerEvents.SERVER_START,
        serverTick = ServerEvents.SERVER_TICK,
        commandRegister = ServerEvents.COMMAND_REGISTER,
    }
}

if allium.environment() == "client" then
    local ClientEvents = require("dev.moongarden.bouquet.api.event.ClientEvents")
    out.client = {
        guiRenderHead = ClientEvents.GUI_RENDER_HEAD,
        guiRenderTail = ClientEvents.GUI_RENDER_TAIL,
    }
end

return out
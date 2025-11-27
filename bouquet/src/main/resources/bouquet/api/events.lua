local CommonEvents = require("dev.hugeblank.bouquet.api.event.CommonEvents")
local ClientEvents = require("dev.hugeblank.bouquet.api.event.ClientEvents")
local ServerEvents = require("dev.hugeblank.bouquet.api.event.ServerEvents")

return {
    common = {
        playerTick = CommonEvents.PLAYER_TICK,
        playerDeath = CommonEvents.PLAYER_DEATH,
        blockInteract = CommonEvents.BLOCK_INTERACT,
        entitySave = CommonEvents.ENTITY_SAVE,
        entityLoad = CommonEvents.ENTITY_LOAD,
    },
    client = {
        guiRenderHead = ClientEvents.GUI_RENDER_HEAD,
        guiRenderTail = ClientEvents.GUI_RENDER_TAIL,
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
-- WAILUA
-- By hugeblank - Jul 9, 2022
-- WAILA-like (What Am I Looking At) script exclusively for the client-side.
-- This is a demonstration of how allium is not just for server sided use cases.

if package.environment ~= "client" then return end

local Component = require("net.minecraft.network.chat.Component")
local BuiltInRegistries = require("net.minecraft.core.registries.BuiltInRegistries")

local renderComponent -- The text to be shared between the render event and tick event

events.client.GUI_RENDER_TAIL:register(script, function(client, context, deltaTracker, gui)
    if renderComponent then -- If there's text, then draw it at the top center
        context:drawCenteredString(gui:getFont(), renderComponent, context:guiWidth()/2, 5, 0xffffffff)
        -- The position 5 was arbitrarily chosen, and was the first value I picked just to test. It worked perfectly.
        -- Exercise for the reader - Create a background frame behind the text, so it can be viewed on white backgrounds.
        -- Note that while text rendering uses RGB, background rendering uses ARGB.
    end
end)

events.common.PLAYER_TICK:register(script, function(player)
    local level = player:level()
    if level:isClientSide() then
        -- Finally, use the block to get the identifier of the block
        local identifier = BuiltInRegistries.BLOCK:getKey(
        -- Use the position to get the state, then the block attributed to that state
                level:getBlockState(
                -- Get the block position the player is looking at <- START HERE read UP ^
                        player:pick(5, 0, false):getBlockPos()
                ):getBlock()
        )
        local namespace, path = identifier:getNamespace(),  identifier:getPath()
        if namespace == "minecraft" and path == "air" then -- If we're just looking at air, don't create a text object.
            renderComponent = nil
        else -- Otherwise, pull the name of the block from the language.
            renderComponent = Component.translatable("block."..namespace.."."..path)
        end
    end
end)
-- Block Inspector
-- Displays info about the block you're looking at near the crosshair,
-- and renders a green 3D outline on the targeted block in the world.

local events = require("bouquet").events

if allium.environment() ~= "client" then return end

local Component = require("net.minecraft.network.chat.Component")
local BuiltInRegistries = require("net.minecraft.core.registries.BuiltInRegistries")
local ShapeRenderer = require("net.minecraft.client.renderer.ShapeRenderer")
local RenderTypes = require("net.minecraft.client.renderer.rendertype.RenderTypes")
local CollisionContext = require("net.minecraft.world.phys.shapes.CollisionContext")

-- Shared state between tick and render
local blockName = nil
local blockId = nil
local blockPosText = nil
local stateProps = nil

-- World render state
local targetPos = nil
local targetShape = nil

-- Gather block info each tick
events.common.playerTick:register(script, function(player)
    local level = player:level()
    if not level:isClientSide() then return end

    local hitResult = player:pick(5, 0, false)
    local pos = hitResult:getBlockPos()
    local state = level:getBlockState(pos)
    local block = state:getBlock()

    local identifier = BuiltInRegistries.BLOCK:getKey(block)
    local namespace = identifier:getNamespace()
    local path = identifier:getPath()

    if namespace == "minecraft" and path == "air" then
        blockName = nil
        blockId = nil
        blockPosText = nil
        stateProps = nil
        targetPos = nil
        targetShape = nil
        return
    end

    blockName = Component.translatable("block." .. namespace .. "." .. path)
    blockId = namespace .. ":" .. path
    blockPosText = pos:getX() .. ", " .. pos:getY() .. ", " .. pos:getZ()

    -- Store position and shape for 3D rendering
    targetPos = pos
    targetShape = state:getShape(level, pos, CollisionContext.empty())

    -- Collect block state properties
    local props = state:getProperties()
    local propLines = {}
    local iter = props:iterator()
    while iter:hasNext() do
        local prop = iter:next()
        local value = state:getValue(prop)
        propLines[#propLines + 1] = prop:getName() .. "=" .. tostring(value)
    end
    if #propLines > 0 then
        stateProps = table.concat(propLines, ", ")
    else
        stateProps = nil
    end
end)

-- Render 3D block outline in the world
events.client.worldRender:register(script, function(client, poseStack, camera, bufferSource, deltaTracker)
    if not targetPos or not targetShape then return end

    local camPos = camera:position()
    local rx = targetPos:getX() - camPos:x()
    local ry = targetPos:getY() - camPos:y()
    local rz = targetPos:getZ() - camPos:z()

    local consumer = bufferSource:getBuffer(RenderTypes.lines())
    -- Green outline (ARGB: 0xFF00FF00), line width 3.0
    ShapeRenderer.renderShape(poseStack, consumer, targetShape, rx, ry, rz, 0xFF00FF00, 3.0)
end)

-- Render HUD overlay near crosshair
events.client.guiRenderTail:register(script, function(client, context, deltaTracker, gui)
    if not blockName then return end

    local font = gui:getFont()
    local cx = context:guiWidth() / 2
    local cy = context:guiHeight() / 2

    -- Position text just below and to the right of the crosshair
    local x = cx + 12
    local y = cy + 12
    local lineHeight = 11

    -- Build lines to display
    local lines = {}
    lines[#lines + 1] = { text = blockName, color = 0xffffffff }
    lines[#lines + 1] = { text = blockId, color = 0xff999999 }
    lines[#lines + 1] = { text = blockPosText, color = 0xffaaaaaa }
    if stateProps then
        lines[#lines + 1] = { text = stateProps, color = 0xff77bbff }
    end

    -- Calculate background box dimensions
    local maxWidth = 0
    for i = 1, #lines do
        local w = font:width(lines[i].text)
        if w > maxWidth then maxWidth = w end
    end

    local padding = 4
    local boxX = x - padding
    local boxY = y - padding
    local boxW = x + maxWidth + padding
    local boxH = y + (#lines * lineHeight) + padding - 2

    -- Draw semi-transparent background (ARGB)
    context:fill(boxX, boxY, boxW, boxH, 0xAA000000)

    -- Draw each line
    for i = 1, #lines do
        context:drawString(font, lines[i].text, x, y + (i - 1) * lineHeight, lines[i].color)
    end
end)

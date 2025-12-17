-- script_a
-- By hugeblank - Mar 12, 2025
-- Very simple script pair for demonstrating cross-script module design.
-- These scripts are primarily used for debugging Allium, but also happen to demonstrate the versatility of `require`,
-- being able to load APIs from other scripts.
-- Note that when require()ing another script, only the value returned from the scripts "static" entrypoint
-- can be obtained. This also means that files in another script cannot be require()d.

local Component = require("net.minecraft.network.chat.Component")

local events = require("bouquet").events

local text

-- Get the string out of the wrapped text object and print it on server start.
-- This is silly, but for debugging Allium's internals invoking game logic is useful.
events.server.serverStart:register(script, function()
    print(text:getString())
end)

-- Return a function for any other script to take and invoke.
-- Note that we could return anything here, beyond just a function.
return function(otherScript, input)
    -- Print our ID, and the invoking script ID
    print(script:getID(), otherScript:getID())
    -- Create a text object to display on server start using a string given by the invoking script.
    text = Component.literal(input)
end
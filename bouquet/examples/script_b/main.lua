-- script_b
-- By hugeblank - Mar 12, 2025
-- Very simple script pair for demonstrating cross-script module design.
-- These scripts are primarily used for debugging Allium, but also happen to demonstrate the versatility of `require`,
-- being able to load APIs from other scripts.

local scripta = require("script_a") -- require script_a, which gives us a function

scripta(script, "Hello script B, this is script A.") -- invoke said function
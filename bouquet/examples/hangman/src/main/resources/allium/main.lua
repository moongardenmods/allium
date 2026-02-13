-- Hangman
-- By hugeblank - March 22, 2022
-- A game of hangman, played in the chat. Use !hangman to start a game, add a letter or word after to start guessing.
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354


local words = require("words")
local Commands = require("net.minecraft.commands.Commands") -- We need the java command manager for creating commands.
local bouquet = require("bouquet")
local arguments = bouquet.command.arguments -- Create shortcut for command argument types
local component = bouquet.component(script)
local fs = bouquet.fs(script)

local active = {}

local leaderboard = {}

local shortcuts = {
    space = { text = " " },
    hangman_start = {
        text = "/hangman start",
        color = "gray",
        click_event = { action = "suggest_command", command = "/hangman start" },
        hover_event = { action = "show_text", value = "Click to suggest" }
    }
}

-- unused helper function to test functions.
-- when a command errors it does not output a stack trace, so we print the error by wrapping a function in this one.
local function debugwrap(func)
    return function(...)
        local out = {pcall(func, ...)}
        if table.remove(out, 1) then
            return table.unpack(out)
        else
            print(table.unpack(out))
        end
    end
end

do -- Load the leaderboard from csv if present
    if fs.exists("leaderboard.csv") then
        local file = fs.open("leaderboard.csv", "r")
        local csv = file:readAll()
        for row in csv:gmatch("[^\n]+") do
            row:gsub("(.+),(.+),(.+),(.+)", function(uuid, username, wins, losses)
                leaderboard[uuid] = {
                    uuid = uuid,
                    username = username,
                    wins = wins,
                    losses = losses
                }
            end)
        end
        file:close()
    end
end

local function flush(player, won)
    local uuid = player:getGameProfile():id():toString()
    if not leaderboard[uuid] then
        leaderboard[uuid] = {
            uuid = uuid,
            username = player:getGameProfile():name(),
            wins = 0,
            losses = 0
        }
    end
    if won then
        leaderboard[uuid].wins = leaderboard[uuid].wins + 1
    else
        leaderboard[uuid].losses = leaderboard[uuid].losses + 1
    end

    local file = fs.open("leaderboard.csv", "w")
    local data = {}
    for uuid, info in pairs(leaderboard) do
        data[#data+1] = uuid..","..info.username..","..info.wins..","..info.losses
    end
    file:write(table.concat(data, "\n"))
    file:close()
end

local function parseGuess(word, guessed)
    -- Returns a string with underscores where a letter hasn't been guessed
    local out = ""
    for i = 1, #word do
        if guessed[i] then
            out = out..word[i]
        else
            out = out.."_"
        end
        if i < #word then
            out = out.." "
        end
    end
    return out
end

local function sendMessage(context, data) -- broadcast a message to the player
    local out = component.fromTable(data)
    context
            :getSource()
            :getPlayer()
            :sendSystemMessage(out)
end

local function sendWin(context, game, player) -- easily broadcast win message
    sendMessage(context, {
        text = "You guessed the word! It was: ",
        color = "green",
        extra = {
            { text = table.concat(game.word), bold = true }
        }
    })
    active[player] = nil
    flush(player, true)
    return 1
end

local function simpleComponent(text, color)
    return { text = text, color = color }
end

local function getInfo(uuid)
    local user = leaderboard[uuid]
    local out = {
        text = "",
        extra = {
            { text = user.username .. " - ", color = "white" },
            {
                text = tostring(user.wins),
                color = "green",
                hover_event = {
                    action = "show_text",
                    value = "Wins"
                }
            },
            shortcuts.space,
            {
                text = tostring(user.losses),
                color = "red",
                hover_event = {
                    action = "show_text",
                    value = "Losses"
                }
            },
            shortcuts.space,
            {
                text = tostring(math.floor(user.wins/(user.wins+user.losses)*100000)/1000).."%",
                color = "yellow",
                hover_event = {
                    action = "show_text",
                    value = "Win rate"
                }
            },
        }
    }
    return out
end

local function getTopPlayers()
    local lb = {}
    for _, v in pairs(leaderboard) do
        lb[#lb+1] = v
    end
    table.sort(lb, function(a, b)
        return a.wins-a.losses > b.wins-b.losses
    end)
    return lb
end

local builder = Commands.literal("hangman") -- Create the builder for the hangman command

bouquet.events.server.commandRegister:register(script, function(_, name, success)
    -- Let us know if the command was successfully registered
    if success and name:find("hangman") then
        print("/hangman command registered!")
    elseif not success and name:find("hangman") then
        print("/hangman command failed to register!")
    end
end)

do
    local c = {
        text = "",
        extra = {
            { text = "A hangman game written entirely in Lua using " },
            {
                text = "",
                extra = {
                    simpleComponent( "All", "light_purple"),
                    simpleComponent( "i", "dark_purple"),
                    simpleComponent( "um", "light_purple"),
                    { text = " & " },
                    simpleComponent("B", "red" ),
                    simpleComponent("o", "gold" ),
                    simpleComponent("u", "yellow" ),
                    simpleComponent("q", "green" ),
                    simpleComponent("u", "aqua" ),
                    simpleComponent("e", "light_purple" ),
                    simpleComponent("t", "dark_purple" ),
                },
                click_event = { action = "open_url", url = "https://github.com/moongardenmods/allium" },
                hover_event = { action = "show_text", value = "Click to view on GitHub"}
            },
            { text = ". Run " },
            shortcuts.hangman_start,
            { text = " to start, guess with " },
            {
                text = "/hangman guess <letter|word>",
                color = "gray",
                click_event = { action = "suggest_command", command = "/hangman guess " },
                hover_event = { action = "show_text", value = "Click to suggest" }
            },
            { text = ". Check out the " },
            {
                text = "source code",
                color = "blue",
                click_event = {
                    action = "open_url",
                    url = "https://github.com/moongardenmods/allium/blob/main/bouquet/examples/hangman/main.lua"
                },
                hover_event = { action = "show_text", value = "Click to view on GitHub"}
            },
            { text = "!" },
        }
    }
    builder:executes(function(context)
        sendMessage(context, c)
        return 1
    end)
end

builder:m_then(Commands.literal("leaderboard"):executes(function(context)
    local player = context:getSource():getPlayer()
    local uuid = player:getGameProfile():id():toString()
    local extra = {  }
    local out = { text = "Your stats:\n", extra = extra }
    if leaderboard[uuid] then
        extra[#extra+1] = {
            text = "",
            extra = {
                getInfo(uuid),
                { text = "\n" }
            }
        }
    else
        extra[#extra+1] = { text = "No data!\n", color = "red" }
    end
    extra[#extra+1] = { text = "Leaderboard:\n" }
    local lb = getTopPlayers()
    for i = 1, 10 do
        local record = lb[i]
        if not record then break end
        local add = { getInfo(record.uuid) }
        if i < 10 then
            add[#add+1] = { text = "\n" }
        end
        extra[#extra+1] = {
            text = i..". ",
            extra = add
        }
    end
    sendMessage(context, out)
    return 1
end))

builder:m_then(Commands.literal("start"):executes(function(context) -- The part of the command with no values attached
    local player = context:getSource():getPlayer()
    if active[player] then -- If there's a game being played tell the player to guess
        context:getSource():sendFailure(text.format("Game already started, use /hangman guess."))
        return 0 -- Execution handlers expect an integer return value.
        -- We use 0 to indicate error, and 1 to indicate success.
    else -- Start a game, since there's not one currently playing
        local game = {
            guesses = 10, -- Give 10 guesses. Could be increased to reduce the difficulty.
            guessed = {}, -- Create a table to mark guessed characters in the word
            word = {}
        }
        local target = words[math.random(1, #words)]:lower()
        for i = 1, target:len() do
            game.word[i] = target:sub(i,i)
            game.guessed[i] = false
        end
        active[player] = game
        sendMessage(context, {
            text = "Guess the word!\n",
            extra = {
                { text = parseGuess(game.word, game.guessed), bold = true },
                { text = "\nYou have "..game.guesses.." guesses. Good luck!" }
            }
        })
        return 1
    end
end))

builder:m_then(Commands.literal("guess"):m_then(Commands.argument("guess", arguments.string.word()):executes(function(context)
    -- The part of the command that handles guesses
    local player = context:getSource():getPlayer()
    local game = active[player]
    local str = arguments.string.getString(context, "guess"):lower() -- Get the guess from the command context
    if game then -- If theres a game running
        sendMessage(context, {
            text = "You guessed ",
            extra = {
                { text = str, bold = true }
            }
        })
        if #str == 1 then -- If the guess is a letter
            local correct = false
            local total = 0 -- Keep track of the total number of letters guessed so far
            for i = 1, #game.word do -- Check the word for the letter
                if game.word[i] == str and not game.guessed[i] then -- If there's a new match
                    correct = true -- Mark the guess as correct
                    game.guessed[i] = true -- Mark all letters in the word that match as guessed
                end
                if game.guessed[i] then total = total + 1 end -- increment total if the current letter has been guessed
            end
            if total == #game.word then -- If all letters have been guessed
                return sendWin(context, game, player)
            elseif correct then -- If the guess was marked as correct
                sendMessage(context, simpleComponent("You guessed a letter correctly!", "green"))
            else
                sendMessage(context, simpleComponent("You guessed a letter incorrectly!", "red"))
                game.guesses = game.guesses-1 -- Subtract a guess
            end
        else -- If the guess is a word
            if str == table.concat(game.word) then -- If the guessed word is an exact match
                return sendWin(context, game, player)
            else -- Otherwise the guess is incorrect
                sendMessage(context, simpleComponent("You guessed incorrectly!", "red"))
                game.guesses = game.guesses-1 -- Subtract a guess
            end
        end
        if game.guesses > 0 then -- So long as the game has guesses left
            local s = " guesses"
            if game.guesses == 1 then s = " guess" end -- Handle the English language
            sendMessage(context, { text = "", extra = {
                { text = parseGuess(game.word, game.guessed), bold = true },
                { text = "\n"..tostring(game.guesses)..s.." left" }
            } })
        else -- No guesses left, game over!
            sendMessage(context, {
                text = "",
                color = "red",
                extra = {
                    { text = "Game over!", bold = true },
                    { text = "The word was: " },
                    { text = table.concat(game.word) }
                }
            })
            active[player] = nil
            flush(player, false)
        end
    else -- No game, tell player how to start one
        context:getSource():sendFailure(component.fromTable({
            text = "No game! ",
            extra ={
                { text = "Use " },
                shortcuts.hangman_start,
                { text = " to play!" }
            }
        }))
    end
    return 1
end)
))

bouquet.command.register(script, builder) -- Register the command
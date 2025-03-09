-- Hangman
-- By hugeblank - March 22, 2022
-- A game of hangman, played in the chat. Use !hangman to start a game, add a letter or word after to start guessing.
-- Derived from the original !hangman command in alpha, for allium-cc
-- Source: https://github.com/hugeblank/Alpha/blob/master/alpha.lua#L354

local words = require("words")
local CommandManager = require("net.minecraft.server.command.CommandManager") -- We need the java command manager for creating commands.
local arguments = command.arguments -- Create shortcut for command argument types

local active = {}

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

local function sendMessage(context, data) -- easily broadcast a message to all players
    local out = text.format(data)
    context
            :getSource()
            :getPlayer()
            :sendMessage(out, false)
end

local builder = CommandManager.literal("hangman") -- Create the builder for the hangman command

events.server.COMMAND_REGISTER:register(script, function(_, name, success)
    -- Let us know if the command was successfully registered
    if success and name:find("hangman") then
        print("/hangman command registered!")
    elseif not success and name:find("hangman") then
        print("/hangman command failed to register!")
    end
end)

builder:executes(function(context)
    local out = {
        "A hangman game written entirely in Lua using <url 'https://github.com/moongardenmods/allium'>",
        "<hover 'Click to view on GitHub'><light_purple>All</light_purple><dark_purple>i</dark_purple>",
        "<light_purple>um</light_purple> & <rainbow>Bouquet</rainbow></hover></url>. Run <gray>",
        "<cmd '/hangman start'><hover 'Click to suggest'>/hangman start</hover></cmd></gray> to start, ",
        "guess with <gray><cmd '/hangman guess'><hover 'Click to suggest'>/hangman guess <letter|word>",
        "</hover></cmd></gray>."
    }
    sendMessage(context, table.concat(out))
    return 1
end)

builder:m_then(CommandManager.literal("start"):executes(function(context) -- The part of the command with no values attached
    local player = context:getSource():getPlayer()
    if active[player] then -- If there's a game being played tell the player to guess
        context:getSource():sendError(text.format("No guess! Add a letter or word to guess"))
        return 0 -- Execution handlers expect an integer return value.
        -- We use 0 to indicate error, and 1 to indicate success.
    else -- Start a game, since there's not one currently playing
        local game = {
            guesses = 10, -- Give 10 guesses. Could be increased to reduce the difficulty.
            guessed = {}, -- Create a table to mark guessed characters in the word
            word = {}
        }
        local target = words[math.random(1, #words)]
        for i = 1, target:len() do
            game.word[i] = target:sub(i,i)
            game.guessed[i] = false
        end
        active[player] = game
        sendMessage(context, string.format(
                "Guess the word!\n<bold>%s</bold>\nYou have %d guesses. Good luck!",
                parseGuess(game.word, game.guessed),
                game.guesses
        ))
        return 1
    end
end))

builder:m_then(CommandManager.literal("guess")
                             :m_then(CommandManager.argument("guess", arguments.string.word())
                                                   :executes(function(context)
    -- The part of the command that handles guesses
    local player = context:getSource():getPlayer()
    local game = active[player]
    local sendWin = function() -- easily broadcast win message
        sendMessage(context, string.format(
                "<green>You guessed the word! It was: <bold>%s</bold></green>",
                table.concat(game.word, "")
        ))
        active[player] = nil
        return 1
    end
    local str = arguments.string.getString(context, "guess") -- Get the guess from the command context
    if game then -- If theres a game running
        sendMessage(context, "You guessed <bold>"..str.."</bold>")
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
                return sendWin()
            elseif correct then -- If the guess was marked as correct
                sendMessage(context, "<green>You guessed a letter correctly!</green>")
            else
                sendMessage(context, "<red>You guessed a letter incorrectly!</red>")
                game.guesses = game.guesses-1 -- Subtract a guess
            end
        else -- If the guess is a word
            if str == table.concat(game.word, "") then -- If the guessed word is an exact match
                return sendWin()
            else -- Otherwise the guess is incorrect
                sendMessage(context, "<red>You guessed incorrectly! </red>")
                game.guesses = game.guesses-1 -- Subtract a guess
            end
        end
        if game.guesses > 0 then -- So long as the game has guesses left
            sendMessage(context, "<bold>"..parseGuess(game.word, game.guessed).."</bold>")
            local s = " guesses"
            if game.guesses == 1 then s = " guess" end -- Handle the English language
            sendMessage(context, tostring(game.guesses)..s.." left")
        else -- No guesses left, game over!
            sendMessage(context, string.format(
                    "<red><bold>Game over!</bold> The word was: <bold>%s</bold></red>",
                    table.concat(game.word, "")
            ))
            active[player] = nil
        end
    else -- No game, tell player how to start one
        context:getSource():sendError(text.format("No game! <gray>Use /hangman start</gray> to play!"))
    end
    return 1
end)
))

command.register(builder) -- Register the command
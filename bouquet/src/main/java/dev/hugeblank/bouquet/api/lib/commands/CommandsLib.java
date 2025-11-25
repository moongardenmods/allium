package dev.hugeblank.bouquet.api.lib.commands;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

// Functionally similar to ComputerCraft's commands API, minus the command indexing
// See: https://github.com/cc-tweaked/CC-Tweaked/blob/mc-1.16.x/src/main/java/dan200/computercraft/shared/computer/apis/CommandAPI.java
@LuaWrapped(name = "commands")
public class CommandsLib implements WrappedLuaLibrary {

    @LuaWrapped
    public void exec(MinecraftServer server, String... args) {

        Commands manager = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack();
        manager.performPrefixedCommand(source, String.join(" ", args));
    }
}

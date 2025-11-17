package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.advancements.criterion.UsedEnderEyeTrigger;

import java.util.Collections;

// Functionally similar to ComputerCraft's commands API
// See: https://github.com/cc-tweaked/CC-Tweaked/blob/mc-1.16.x/src/main/java/dan200/computercraft/shared/computer/apis/CommandAPI.java
@LuaWrapped(name = "commands")
public class CommandsLib implements WrappedLuaLibrary {

    @LuaWrapped
    public void exec(MinecraftServer server, String... args) {

        Commands manager = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack();
        manager.performPrefixedCommand(source, String.join(" ", args));
    }

    @LuaIndex
    public BoundCommand index(MinecraftServer server, String command) {

        Commands manager = server.getCommands();
        CommandSourceStack source = server.createCommandSourceStack();
        CommandDispatcher<CommandSourceStack> dispatcher = manager.getDispatcher();
        CommandNode<?> node = dispatcher.findNode(Collections.singleton(command));

        if (node == null) return null;
        else return (args) -> manager.performPrefixedCommand(source, (command + " " + String.join(" ", args).trim()));
    }

    @FunctionalInterface
    public interface BoundCommand {
        void exec(String... args);
    }
}

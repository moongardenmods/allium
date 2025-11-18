package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.CoerceToBound;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.hugeblank.bouquet.api.lib.AlliumLib.COMMANDS;

@LuaWrapped(name = "command")
public class CommandLib implements WrappedLuaLibrary {
    private final Script script;

    public CommandLib(Script script) {
        this.script = script;
    }

    @LuaWrapped
    public void register(LiteralArgumentBuilder<CommandSourceStack> builder, @OptionalArg Commands.CommandSelection environment) {
        COMMANDS.add(new CommandRegisterEntry(
                script,
                builder,
                environment == null ? Commands.CommandSelection.ALL : environment
        ));
    }

    @LuaWrapped(name = "arguments")
    public static final @CoerceToBound ArgumentTypeLib ARGUMENTS = new ArgumentTypeLib();
}

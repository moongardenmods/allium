package dev.hugeblank.bouquet.api.event;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public record CommandRegisterEntry(
        Script script,
        LiteralArgumentBuilder<CommandSourceStack> builder,
        Commands.CommandSelection environment
        ) {}

package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.advancements.criterion.UsedEnderEyeTrigger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public record CommandRegisterEntry(
        Script script,
        LiteralArgumentBuilder<CommandSourceStack> builder,
        Commands.CommandSelection environment
        ) {}

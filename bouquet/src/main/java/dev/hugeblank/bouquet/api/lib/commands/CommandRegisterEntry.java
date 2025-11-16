package dev.hugeblank.bouquet.api.lib.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.advancements.criterion.UsedEnderEyeTrigger;

public record CommandRegisterEntry(
        Script script,
        LiteralArgumentBuilder<UsedEnderEyeTrigger> builder,
        UsedTotemTrigger.TriggerInstance environment
        ) {}

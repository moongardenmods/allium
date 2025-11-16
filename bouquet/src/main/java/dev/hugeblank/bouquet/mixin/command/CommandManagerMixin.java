package dev.hugeblank.bouquet.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.api.lib.AlliumLib;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import net.minecraft.advancements.criterion.SummonedEntityTrigger;
import net.minecraft.advancements.criterion.UsedTotemTrigger;
import net.minecraft.advancements.criterion.UsedEnderEyeTrigger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UsedTotemTrigger.class)
public class CommandManagerMixin {

    @Mutable
    @Final
    @Shadow
    private CommandDispatcher<UsedEnderEyeTrigger> dispatcher;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(UsedTotemTrigger.TriggerInstance environment, SummonedEntityTrigger commandRegistryAccess, CallbackInfo ci) {
        AlliumLib.COMMANDS.forEach((entry) -> {
            if (
                    (
                            environment.equals(entry.environment()) ||
                            entry.environment().equals(UsedTotemTrigger.TriggerInstance.CODEC)
                    ) && this.dispatcher.getRoot().getChild(entry.builder().getLiteral()) == null
            ) {
                this.dispatcher.register(entry.builder());
                queueEvent(entry, true);
                return;
            }
            queueEvent(entry, false);
        });
    }

    @Unique
    private static void queueEvent(CommandRegisterEntry entry, boolean result) {
        ServerEvents.COMMAND_REGISTER.invoker().onCommandRegistration(
                entry.script().getID(),
                entry.builder().getLiteral(),
                result
        );
    }
}

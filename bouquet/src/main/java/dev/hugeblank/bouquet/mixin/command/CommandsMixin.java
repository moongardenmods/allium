package dev.hugeblank.bouquet.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.api.lib.commands.CommandLib;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {

    @Mutable
    @Final
    @Shadow
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(Commands.CommandSelection commandSelection, CommandBuildContext context, CallbackInfo ci) {
        CommandLib.COMMANDS.forEach((entry) -> {
            if (
                    (
                            commandSelection.equals(entry.environment()) ||
                            entry.environment().equals(Commands.CommandSelection.ALL)
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

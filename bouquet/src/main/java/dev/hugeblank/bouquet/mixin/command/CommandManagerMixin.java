package dev.hugeblank.bouquet.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.api.lib.AlliumLib;
import dev.hugeblank.bouquet.api.lib.commands.CommandRegisterEntry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Mutable
    @Final
    @Shadow
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        AlliumLib.COMMANDS.forEach((entry) -> {
            if (
                    (
                            environment.equals(entry.environment()) ||
                            entry.environment().equals(CommandManager.RegistrationEnvironment.ALL)
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

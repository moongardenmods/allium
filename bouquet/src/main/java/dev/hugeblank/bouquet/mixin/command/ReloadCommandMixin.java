package dev.hugeblank.bouquet.mixin.command;

import com.mojang.brigadier.context.CommandContext;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import net.minecraft.server.command.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {
    @Inject(at = @At("HEAD"), method = "method_13530(Lcom/mojang/brigadier/context/CommandContext;)I")
    private static void executes(CommandContext<?> context, CallbackInfoReturnable<Integer> cir) {
        ScriptRegistry.COMMON.reloadAll();
        ScriptRegistry.DEDICATED.reloadAll();
    }
}

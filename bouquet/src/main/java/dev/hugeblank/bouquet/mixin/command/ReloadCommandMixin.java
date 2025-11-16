package dev.hugeblank.bouquet.mixin.command;

import com.mojang.brigadier.context.CommandContext;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientboundPlayerPositionPacket.class)
public class ReloadCommandMixin {
    // TODO(Ravel): target method method_13530 with the signature not found
    @Inject(at = @At("HEAD"), method = "method_13530(Lcom/mojang/brigadier/context/CommandContext;)I")
    private static void executes(CommandContext<?> context, CallbackInfoReturnable<Integer> cir) {
        ScriptRegistry.COMMON.reloadAll();
        ScriptRegistry.DEDICATED.reloadAll();
    }
}

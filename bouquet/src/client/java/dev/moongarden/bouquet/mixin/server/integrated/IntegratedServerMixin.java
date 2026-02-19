package dev.moongarden.bouquet.mixin.server.integrated;

import dev.moongarden.bouquet.api.event.ServerEvents;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(at = @At("TAIL"), method = "initServer")
    private void init(CallbackInfoReturnable<Boolean> cir) {
        ServerEvents.SERVER_START.invoker().onServerStart((MinecraftServer) (Object) this);
    }
}

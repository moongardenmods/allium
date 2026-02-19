package dev.moongarden.bouquet.mixin.entity;

import dev.moongarden.bouquet.api.event.CommonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(at = @At("TAIL"), method = "tick")
    private void tick(CallbackInfo ci) {
        CommonEvents.PLAYER_TICK.invoker().onPlayerTick((Player) (Object) this);
    }

    @Inject(at = @At("TAIL"), method = "die")
    private void die(DamageSource source, CallbackInfo ci) {
        CommonEvents.PLAYER_DEATH.invoker().onPlayerDeath((Player) (Object) this, source);
    }
}

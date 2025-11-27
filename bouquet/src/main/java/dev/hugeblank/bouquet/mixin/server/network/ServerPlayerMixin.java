package dev.hugeblank.bouquet.mixin.server.network;

import com.mojang.authlib.GameProfile;
import dev.hugeblank.bouquet.api.event.ServerEvents;
import dev.hugeblank.bouquet.util.EntityDataHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(at = @At("HEAD"), method = "initInventoryMenu")
    private void onPlayerConnect(CallbackInfo ci) {
        ServerEvents.PLAYER_JOIN.invoker().onPlayerJoin((ServerPlayer) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "disconnect")
    private void onDisconnect(CallbackInfo ci) {
        ServerEvents.PLAYER_QUIT.invoker().onPlayerQuit((ServerPlayer) (Object) this);
    }

    @Inject(at = @At("HEAD"), method = "onInsideBlock")
    private void onBlockCollision(BlockState state, CallbackInfo ci) {
        ServerEvents.PLAYER_BLOCK_COLLISION.invoker().onPlayerBlockCollision((ServerPlayer) (Object) this, state);
    }

    @Inject(at = @At("RETURN"), method = "restoreFrom")
    private void allium_private$copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ((EntityDataHolder) this).allium_private$copyTempData(oldPlayer);
    }
}

package dev.hugeblank.bouquet.mixin.server;

import dev.hugeblank.bouquet.api.event.ServerEvents;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(PingPacketTypes.class)
public class PlayerManagerMixin {

    @Inject(at = @At("HEAD"), method = "broadcast(Lnet/minecraft/gametest/framework/GlobalTestReporter;Ljava/util/function/Predicate;Lnet/minecraft/network/protocol/game/ServerboundChangeGameModePacket;Lnet/minecraft/network/message/MessageType$Parameters;)V")
    private void onChatMessage(GlobalTestReporter message, Predicate<ServerboundChangeGameModePacket> shouldSendFiltered, ServerboundChangeGameModePacket sender, GameTestAssertPosException.Parameters params, CallbackInfo ci) {
        var msg = message.signedBody().content();
        ServerEvents.CHAT_MESSAGE.invoker().onChatMessage(sender, msg);
    }

}

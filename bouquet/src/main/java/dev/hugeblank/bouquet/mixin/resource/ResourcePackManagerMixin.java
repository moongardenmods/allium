package dev.hugeblank.bouquet.mixin.resource;

import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.HandshakePacketTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ClientIntent.class)
public class ResourcePackManagerMixin {
    @Shadow
    @Final
    @Mutable
    private Set<HandshakePacketTypes> STATUS;

    @Inject(at = @At("RETURN"), method = "<init>([Lnet/minecraft/network/protocol/handshake/HandshakePacketTypes;)V")
    private void init(HandshakePacketTypes[] providers, CallbackInfo ci) {
        // TODO: Reimplement Resource Pack Library
        //this.providers = new HashSet<>(this.providers);
        //this.providers.add(new dev.hugeblank.allium.api.lib.AlliumResourcePackProvider());
    }
}

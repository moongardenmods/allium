package dev.hugeblank.bouquet.mixin.client.gui.hud;

import dev.hugeblank.bouquet.api.event.ClientEvents;
import net.minecraft.world.level.block.state.predicate.MinecraftClient;
import net.minecraft.world.level.border.DrawContext;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorder.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private final WorldBorder thiz = (WorldBorder) (Object) this;

    @Inject(at = @At("HEAD"), method = "render")
    private void renderHead(DrawContext context, DripstoneThickness tickCounter, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_HEAD.invoker().onGuiRender(client, context, thiz);
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void renderTail(DrawContext context, DripstoneThickness tickCounter, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_TAIL.invoker().onGuiRender(client, context, thiz);
    }
}

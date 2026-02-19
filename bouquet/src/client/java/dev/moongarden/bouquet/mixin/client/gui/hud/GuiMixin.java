package dev.moongarden.bouquet.mixin.client.gui.hud;

import dev.moongarden.bouquet.api.event.ClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private final Gui thiz = (Gui) (Object) this;

    @Inject(at = @At("HEAD"), method = "render")
    private void renderHead(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_HEAD.invoker().onGuiRender(minecraft, context, deltaTracker, thiz);
    }

    @Inject(at = @At("TAIL"), method = "render")
    private void renderTail(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientEvents.GUI_RENDER_TAIL.invoker().onGuiRender(minecraft, context, deltaTracker, thiz);
    }
}

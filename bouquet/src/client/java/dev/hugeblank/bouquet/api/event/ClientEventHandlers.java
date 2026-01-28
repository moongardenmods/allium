package dev.hugeblank.bouquet.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

// For all events that use classes that the server doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

public class ClientEventHandlers {

    public interface GuiRender {
        void onGuiRender(Minecraft client, GuiGraphics context,  DeltaTracker deltaTracker, Gui hud);
    }

    public interface WorldRender {
        void onWorldRender(Minecraft client, PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource bufferSource, DeltaTracker deltaTracker);
    }
}

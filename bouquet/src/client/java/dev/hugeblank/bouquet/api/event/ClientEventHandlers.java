package dev.hugeblank.bouquet.api.event;

import net.minecraft.world.level.block.state.predicate.MinecraftClient;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.border.DrawContext;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

// For all events that use classes that the server doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

public class ClientEventHandlers {

    public interface GuiRender {
        void onGuiRender(MinecraftClient client, DrawContext context, WorldBorder hud);
    }
}

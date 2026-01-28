package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.api.event.SimpleEventType;

public class ClientEvents {
    // The end of the client render cycle (renders below everything in the gui)
    public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_HEAD = new SimpleEventType<>();
    // The end of the client render cycle (renders above everything in the gui)
    public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_TAIL = new SimpleEventType<>();
    // Fires after the world has been rendered, allowing 3D overlays to be drawn in world-space
    public static final SimpleEventType<ClientEventHandlers.WorldRender> WORLD_RENDER = new SimpleEventType<>();
}

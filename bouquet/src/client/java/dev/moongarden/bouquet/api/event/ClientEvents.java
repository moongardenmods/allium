package dev.moongarden.bouquet.api.event;

import dev.moongarden.allium.api.event.SimpleEventType;

public class ClientEvents {
    // The end of the client render cycle (renders below everything in the gui)
    public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_HEAD = new SimpleEventType<>();
    // The end of the client render cycle (renders above everything in the gui)
    public static final SimpleEventType<ClientEventHandlers.GuiRender> GUI_RENDER_TAIL = new SimpleEventType<>();
}

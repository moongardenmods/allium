package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;

@LuaWrapped
public class CommonEvents implements Events {

    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerTick> PLAYER_TICK;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerDeath> PLAYER_DEATH;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockInteract> BLOCK_INTERACT;

    static {
        // player gets ticked
        PLAYER_TICK = new SimpleEventType<>(ServerConfigurationPacketListener.of("allium:common/player_tick"));
        // player dies
        PLAYER_DEATH = new SimpleEventType<>(ServerConfigurationPacketListener.of("allium:common/player_death"));
        // player interacts (right clicks) with a block
        BLOCK_INTERACT = new SimpleEventType<>(ServerConfigurationPacketListener.of("allium:common/block_interact"));


    }
}

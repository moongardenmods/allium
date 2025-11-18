package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.api.event.SimpleEventType;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.resources.Identifier;

@LuaWrapped
public class CommonEvents implements Events {

    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerTick> PLAYER_TICK;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerDeath> PLAYER_DEATH;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.PlayerBlockInteract> BLOCK_INTERACT;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.EntitySave> ENTITY_SAVE;
    @LuaWrapped public static final SimpleEventType<CommonEventHandlers.EntityLoad> ENTITY_LOAD;

    static {
        // player gets ticked
        PLAYER_TICK = new SimpleEventType<>(Identifier.parse("allium:common/player_tick"));
        // player dies
        PLAYER_DEATH = new SimpleEventType<>(Identifier.parse("allium:common/player_death"));
        // player interacts (right clicks) with a block
        BLOCK_INTERACT = new SimpleEventType<>(Identifier.parse("allium:common/block_interact"));
        // entity gets saved
        ENTITY_SAVE = new SimpleEventType<>(Identifier.parse("allium:common/entity_save"));
        // entity gets loaded
        ENTITY_LOAD = new SimpleEventType<>(Identifier.parse("allium:common/entity_load"));
    }
}

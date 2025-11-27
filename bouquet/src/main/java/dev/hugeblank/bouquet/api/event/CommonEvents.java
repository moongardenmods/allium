package dev.hugeblank.bouquet.api.event;

import dev.hugeblank.allium.api.event.SimpleEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

public class CommonEvents {
    /// player gets ticked
    public static final SimpleEventType<PlayerTick> PLAYER_TICK = new SimpleEventType<>();
    /// player dies
    public static final SimpleEventType<PlayerDeath> PLAYER_DEATH = new SimpleEventType<>();
    /// player interacts (right clicks) with a block
    public static final SimpleEventType<PlayerBlockInteract> BLOCK_INTERACT = new SimpleEventType<>();
    /// entity gets saved
    public static final SimpleEventType<EntitySave> ENTITY_SAVE = new SimpleEventType<>();
    /// entity gets loaded
    public static final SimpleEventType<EntityLoad> ENTITY_LOAD = new SimpleEventType<>();

    public interface PlayerTick {
        void onPlayerTick(Player player);
    }

    public interface PlayerBlockInteract {
        void onPlayerBlockInteraction(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult);
    }

    public interface PlayerDeath {
        void onPlayerDeath(Player player, DamageSource damageSource);
    }

    public interface EntitySave {
        void save(ValueOutput valueOutput);
    }

    public interface EntityLoad {
        void load(ValueInput valueInput);
    }
}

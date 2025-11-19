package dev.hugeblank.bouquet.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

public class CommonEventHandlers {
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

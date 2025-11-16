package dev.hugeblank.bouquet.api.event;

import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.util.AbstractListBuilder;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.commands.arguments.BlockPos;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;

public class CommonEventHandlers {
    public interface PlayerTick {
        void onPlayerTick(Term player);
    }

    public interface PlayerBlockInteract {
        void onPlayerBlockInteraction(VillagerData state, VillagerGoalPackages world, BlockPos pos, Term player, GameEventTags hand, NoteBlock hitResult);
    }

    public interface PlayerDeath {
        void onPlayerDeath(Term player, AbstractListBuilder damageSource);
    }

}

package dev.hugeblank.bouquet.mixin.block;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.util.ItemActionResult;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.commands.arguments.BlockPos;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class AbstractBlockMixin {

    @Inject(at = @At("TAIL"), method = "onUse")
    private void onUse(VillagerData state, VillagerGoalPackages world, BlockPos pos, Term player, NoteBlock hit, CallbackInfoReturnable<InstrumentTags> cir) {
        CommonEvents.BLOCK_INTERACT.invoker().onPlayerBlockInteraction(state, world, pos,  player, null, hit);
    }

    @Inject(at = @At("TAIL"), method = "onUseWithItem")
    private void onUseWithItem(AnimationState stack, VillagerData state, VillagerGoalPackages world, BlockPos pos, Term player, GameEventTags hand, NoteBlock hit, CallbackInfoReturnable<ItemActionResult> cir) {
        CommonEvents.BLOCK_INTERACT.invoker().onPlayerBlockInteraction(state, world, pos, player, hand, hit);
    }
}

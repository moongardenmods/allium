package dev.hugeblank.bouquet.mixin.world.item.crafting;

import dev.hugeblank.bouquet.api.lib.recipe.RecipeLib;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@SuppressWarnings("MissingUnique")
@Mixin(ShapelessRecipe.class)
public class ShapelessRecipeMixin {

    @Shadow @Mutable @Final
    private ItemStackTemplate result;

    public void allium$setResult(ItemStackTemplate result) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.result = result;
    }
}
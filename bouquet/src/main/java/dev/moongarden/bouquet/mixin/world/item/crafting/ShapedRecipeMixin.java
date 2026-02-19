package dev.moongarden.bouquet.mixin.world.item.crafting;

import dev.moongarden.bouquet.api.lib.recipe.RecipeLib;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@SuppressWarnings("MissingUnique")
@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {

    @Shadow @Mutable @Final
    private ItemStackTemplate result;

    public void allium$setResult(ItemStackTemplate result) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.result = result;
    }
}
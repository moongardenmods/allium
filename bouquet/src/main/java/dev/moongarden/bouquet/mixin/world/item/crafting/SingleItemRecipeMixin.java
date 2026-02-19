package dev.moongarden.bouquet.mixin.world.item.crafting;


import dev.moongarden.bouquet.api.lib.recipe.RecipeLib;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@SuppressWarnings("MissingUnique")
@Mixin(SingleItemRecipe.class)
public class SingleItemRecipeMixin {
    @Shadow @Mutable @Final
    private Ingredient input;

    @Shadow @Mutable @Final
    private ItemStackTemplate result;

    public Ingredient allium$getInput() {
        return input;
    }

    public void allium$setInput(Ingredient input) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.input = input;
    }

    public void allium$setResult(ItemStackTemplate result) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.result = result;
    }
}

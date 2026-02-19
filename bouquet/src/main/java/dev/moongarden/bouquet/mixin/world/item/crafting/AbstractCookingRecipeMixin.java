package dev.moongarden.bouquet.mixin.world.item.crafting;

import dev.moongarden.bouquet.api.lib.recipe.RecipeLib;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@SuppressWarnings("MissingUnique")
@Mixin(AbstractCookingRecipe.class)
public class AbstractCookingRecipeMixin {
    @Shadow @Mutable @Final
    protected AbstractCookingRecipe.CookingBookInfo bookInfo;

    @Shadow @Mutable @Final
    private float experience;

    @Shadow @Mutable @Final
    private int cookingTime;

    public void allium$setBookInfo(AbstractCookingRecipe.CookingBookInfo bookInfo) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.bookInfo = bookInfo;
    }

    public void allium$setExperience(float experience) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.experience = experience;
    }

    public void allium$setCookingTime(int cookTime) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.cookingTime = cookTime;
    }
}
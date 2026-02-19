package dev.moongarden.bouquet.mixin.world.item.crafting;

import dev.moongarden.bouquet.api.lib.recipe.RecipeLib;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.squiddev.cobalt.LuaError;

@Mixin(NormalCraftingRecipe.class)
public class NormalCraftingRecipeMixin {
    @Shadow
    @Mutable
    @Final
    protected CraftingRecipe.CraftingBookInfo bookInfo;

    public void allium$setBookInfo(CraftingRecipe.CraftingBookInfo bookInfo) throws LuaError {
        RecipeLib.INSTANCE.assertInModifyPhase();

        this.bookInfo = bookInfo;
    }
}

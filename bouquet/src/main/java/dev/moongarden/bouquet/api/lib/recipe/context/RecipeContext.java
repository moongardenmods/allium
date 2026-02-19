package dev.moongarden.bouquet.api.lib.recipe.context;

import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.*;

import java.util.Collection;

public abstract class RecipeContext {
    protected final RecipeMap recipes;

    public RecipeContext(RecipeMap recipes) {
        this.recipes = recipes;
    }

    @LuaWrapped
    public Recipe<?> getRecipe(Identifier id) {
        return getRecipe(ResourceKey.create(Registries.RECIPE, id));
    }

    @LuaWrapped
    public Recipe<?> getRecipe(ResourceKey<Recipe<?>> rkey) {
        RecipeHolder<?> holder = recipes.byKey(rkey);
        if (holder == null) {
            return null;
        }
        return holder.value();
    }

    @LuaWrapped
    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> getRecipesOfType(RecipeType<T> type) {
        return recipes.byType(type);
    }

    protected Pair<ResourceKey<Recipe<?>>, RecipeHolder<Recipe<?>>> createPair(Identifier id, Recipe<?> recipe) {
        ResourceKey<Recipe<?>> rkey = ResourceKey.create(Registries.RECIPE, id);
        RecipeHolder<Recipe<?>> holder = new RecipeHolder<>(rkey, recipe);
        return new Pair<>(rkey, holder);
    }
}
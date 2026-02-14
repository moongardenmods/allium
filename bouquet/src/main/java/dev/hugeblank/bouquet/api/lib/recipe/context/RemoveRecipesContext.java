package dev.hugeblank.bouquet.api.lib.recipe.context;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.bouquet.mixin.RecipeMapAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.squiddev.cobalt.LuaError;

import java.util.Map;

@LuaWrapped
public class RemoveRecipesContext extends RecipeContext {
    public RemoveRecipesContext(RecipeMap recipes) {
        super(recipes);
    }

    @LuaWrapped
    public void removeRecipe(Identifier id) throws LuaError {
        removeRecipe(ResourceKey.create(Registries.RECIPE, id));
    }

    @LuaWrapped
    public void removeRecipe(ResourceKey<Recipe<?>> rkey) throws LuaError {
        var oldRecipe = ((RecipeMapAccessor) recipes).getByKey().remove(rkey);

        if (oldRecipe == null)
            throw new LuaError("recipe '" + rkey + "' doesn't exist");

        ((RecipeMapAccessor) recipes).getByType().get(oldRecipe.value().getType()).remove(oldRecipe);
    }

    @LuaWrapped
    public void removeRecipe(Recipe<?> recipe) throws LuaError {
        for (Map.Entry<ResourceKey<Recipe<?>>, RecipeHolder<?>> entry : ((RecipeMapAccessor) recipes).getByKey().entrySet()) {
            if (entry.getValue().value().equals(recipe)) {
                removeRecipe(entry.getKey());
            }
        }
    }

    public interface Handler {
        void removeRecipes(RemoveRecipesContext ctx);
    }
}
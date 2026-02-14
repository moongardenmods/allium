package dev.hugeblank.bouquet.api.lib.recipe.context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.hugeblank.allium.api.LuaStateArg;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.util.Pair;
import dev.hugeblank.bouquet.mixin.RecipeMapAccessor;
import dev.hugeblank.bouquet.util.LuaOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

@LuaWrapped
public class ModifyRecipesContext extends RecipeContext {
    private final HolderLookup.Provider registries;
    public ModifyRecipesContext(RecipeMap recipes, HolderLookup.Provider registries) {
        super(recipes);
        this.registries = registries;
    }

    @LuaWrapped
    public void replaceRecipe(Identifier id, String json) throws LuaError {
        replaceRecipe(id, JsonParser.parseString(json).getAsJsonObject());
    }

    @LuaWrapped
    public void replaceRecipe(Identifier id, JsonObject el) throws LuaError {
        replaceRecipe(id, el, JsonOps.INSTANCE);
    }

    @LuaWrapped
    public void replaceRecipe(@LuaStateArg LuaState state, Identifier id, LuaValue val) throws LuaError {
        replaceRecipe(id, val, new LuaOps(state));
    }

    @LuaWrapped
    public <T> void replaceRecipe(Identifier id, T val, DynamicOps<T> ops) throws LuaError {
        final LuaError[] err = new LuaError[1];
        RegistryOps<T> internalOps = registries.createSerializationContext(ops);
        Recipe.CODEC.parse(internalOps, val).ifSuccess((recipe -> {
                    try {
                        replaceRecipe(id, recipe);
                    } catch (LuaError e) {
                        err[0] = e;
                    }
                })
        );
        if (err[0] != null) throw err[0];
    }

    @LuaWrapped
    public void replaceRecipe(Identifier id, Recipe<?> newRecipe) throws LuaError {
        Pair<ResourceKey<Recipe<?>>, RecipeHolder<Recipe<?>>> pair = createPair(id, newRecipe);
        RecipeHolder<?> oldRecipe = ((RecipeMapAccessor) recipes).getByKey().put(pair.left(), pair.right());

        if (oldRecipe == null)
            throw new LuaError("recipe '" + id + "' doesn't exist");
        else if (oldRecipe.value().getType() != newRecipe.getType())
            throw new LuaError("old recipe and new recipe's types don't match");

        ((RecipeMapAccessor) recipes).getByType().put(newRecipe.getType(), pair.right());
    }

    public interface Handler {
        void modifyRecipes(ModifyRecipesContext ctx);
    }
}
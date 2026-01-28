package dev.hugeblank.bouquet.api.lib.recipe.context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.bouquet.mixin.RecipeMapAccessor;
import dev.hugeblank.bouquet.util.LuaOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

@LuaWrapped
public class AddRecipesContext extends RecipeContext {
    private final HolderLookup.Provider registries;

    public AddRecipesContext(RecipeMap recipes, HolderLookup.Provider registries) {
        super(recipes);
        this.registries = registries;
    }

    @LuaWrapped
    public void addRecipe(Identifier id, String json) throws LuaError {
        addRecipe(id, JsonParser.parseString(json).getAsJsonObject());
    }

    @LuaWrapped
    public void addRecipe(Identifier id, JsonObject el) throws LuaError {
        addRecipe(id, el, JsonOps.INSTANCE);
    }

    @LuaWrapped
    public void addRecipe(@LuaStateArg LuaState state, Identifier id, LuaValue val) throws LuaError {
        addRecipe(id, val, new LuaOps(state));
    }

    @LuaWrapped
    public <T> void addRecipe(Identifier id, T val, DynamicOps<T> ops) throws LuaError {
        final LuaError[] err = new LuaError[1];
        RegistryOps<T> internalOps = registries.createSerializationContext(ops);
        Recipe.CODEC.parse(internalOps, val).ifSuccess((recipe -> {
                    try {
                        addRecipe(id, recipe);
                    } catch (LuaError e) {
                        err[0] = e;
                    }
                })
        );
        if (err[0] != null) throw err[0];
    }

    @LuaWrapped
    public void addRecipe(Identifier id, Recipe<?> recipe) throws LuaError {
        var pair = createPair(id, recipe);
        if (((RecipeMapAccessor) recipes).getByKey().put(pair.left(), pair.right()) != null) {
            throw new LuaError("recipe '" + id + "' already exists");
        }
        ((RecipeMapAccessor) recipes).getByType().put(recipe.getType(), pair.right());
    }

    public interface Handler {
        void addRecipes(AddRecipesContext ctx);
    }
}
package dev.hugeblank.bouquet.api.lib.recipe;

import dev.hugeblank.allium.api.event.SimpleEventType;
import dev.hugeblank.allium.api.CoerceToBound;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.bouquet.api.lib.recipe.context.AddRecipesContext;
import dev.hugeblank.bouquet.api.lib.recipe.context.ModifyRecipesContext;
import dev.hugeblank.bouquet.api.lib.recipe.context.RemoveRecipesContext;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeMap;
import org.squiddev.cobalt.LuaError;

public class RecipeLib {
    public static RecipeLib INSTANCE = new RecipeLib();

    private static boolean IN_MODIFY_PHASE = false;

    @LuaWrapped
    public final SimpleEventType<AddRecipesContext.Handler> ADD = new SimpleEventType<>(EClass.fromJava(AddRecipesContext.Handler.class));

    @LuaWrapped
    public final SimpleEventType<ModifyRecipesContext.Handler> MODIFY = new SimpleEventType<>(EClass.fromJava(ModifyRecipesContext.Handler.class));

    @LuaWrapped
    public final SimpleEventType<RemoveRecipesContext.Handler> REMOVE = new SimpleEventType<>(EClass.fromJava(RemoveRecipesContext.Handler.class));

    @LuaWrapped(name = "types")
    public final @CoerceToBound RecipeTypeLib TYPES = new RecipeTypeLib();

    public void assertInModifyPhase() throws LuaError {
        if (!IN_MODIFY_PHASE) {
            throw new LuaError("tried to modify recipe not in modify phase");
        }
    }

    public void runRecipeEvents(RecipeMap recipes, HolderLookup.Provider registries) {
        AddRecipesContext addCtx = new AddRecipesContext(recipes, registries);

        ADD.invoker().addRecipes(addCtx);

        ModifyRecipesContext modifyCtx = new ModifyRecipesContext(recipes, registries);

        IN_MODIFY_PHASE = true;
        MODIFY.invoker().modifyRecipes(modifyCtx);
        IN_MODIFY_PHASE = false;

        RemoveRecipesContext removeCtx = new RemoveRecipesContext(recipes);

        REMOVE.invoker().removeRecipes(removeCtx);
    }
}
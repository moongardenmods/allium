package dev.hugeblank.bouquet.api.lib.recipe;

import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;

public class RecipeTypeLib {
    @LuaIndex
    public RecipeType<?> index(String type) {
        Identifier id = Identifier.parse(type);

        return BuiltInRegistries.RECIPE_TYPE.getOptional(id).orElse(null);
    }
}
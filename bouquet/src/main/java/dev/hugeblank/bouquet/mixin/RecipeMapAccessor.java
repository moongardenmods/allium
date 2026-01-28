package dev.hugeblank.bouquet.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RecipeMap.class)
public interface RecipeMapAccessor {
    @Accessor
    Multimap<RecipeType<?>, RecipeHolder<?>> getByType();

    @Accessor
    Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> getByKey();
}

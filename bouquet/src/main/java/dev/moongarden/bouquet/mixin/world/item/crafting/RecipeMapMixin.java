package dev.moongarden.bouquet.mixin.world.item.crafting;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeMap.class)
public class RecipeMapMixin {
    @WrapOperation(method = "create", at = @At(value = "NEW", target = "(Lcom/google/common/collect/Multimap;Ljava/util/Map;)Lnet/minecraft/world/item/crafting/RecipeMap;"))
    private static RecipeMap makeMutable(Multimap<RecipeType<?>, RecipeHolder<?>> byType, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey, Operation<RecipeMap> original) {
        return original.call(HashMultimap.create(byType), new HashMap<>(byKey));
    }
}

package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@LuaWrapped(name = "game")
public class GameLib implements WrappedLuaLibrary {

    @LuaWrapped
    public Block getBlock(String id) {
        Optional<Holder.Reference<@NotNull Block>> ref = BuiltInRegistries.BLOCK.get(Identifier.parse(id));
        return ref.map(Holder.Reference::value).orElse(null);
    }

    @LuaWrapped
    public Item getItem(String id) {
        Optional<Holder.Reference<@NotNull Item>> ref = BuiltInRegistries.ITEM.get(Identifier.parse(id));
        return ref.map(Holder.Reference::value).orElse(null);
    }

    @LuaWrapped
    public ServerLevel getLevel(MinecraftServer server, String id) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(id)));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, Block> listBlocks() {
        return BuiltInRegistries.BLOCK.stream().collect(Collectors.toMap(x -> BuiltInRegistries.BLOCK.getKey(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, Item> listItems() {
        return BuiltInRegistries.ITEM.stream().collect(Collectors.toMap(x -> BuiltInRegistries.ITEM.getKey(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<Identifier, ServerLevel> listLevels(MinecraftServer server) {
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .collect(Collectors.toMap(x -> x.dimension().identifier(), x -> x));
    }
}

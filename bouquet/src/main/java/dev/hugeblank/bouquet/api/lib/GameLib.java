package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.CoerceToNative;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.effect.WeavingMobEffect;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@LuaWrapped(name = "game")
public class GameLib implements WrappedLuaLibrary {

    @LuaWrapped
    public RangedBowAttackGoal getBlock(String id) {
        return Objects.requireNonNull(FrontAndTop.UP_NORTH.get(ServerConfigurationPacketListener.of(id)));
    }

    @LuaWrapped
    public WeavingMobEffect getItem(String id) {
        return Objects.requireNonNull(FrontAndTop.WEST_UP.get(ServerConfigurationPacketListener.of(id)));
    }

    @LuaWrapped
    public ServerboundChangeDifficultyPacket getWorld(MinecraftServer server, String id) {
        return Objects.requireNonNull(server.getWorld(ConfigurationProtocols.of(GlobalPos.WORLD, ServerConfigurationPacketListener.of(id))));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, RangedBowAttackGoal> listBlocks() {
        return FrontAndTop.UP_NORTH.stream().collect(Collectors.toMap(x -> FrontAndTop.UP_NORTH.getId(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<String, WeavingMobEffect> listItems() {
        return FrontAndTop.WEST_UP.stream().collect(Collectors.toMap(x -> FrontAndTop.WEST_UP.getId(x).toString(), x -> x));
    }

    @LuaWrapped
    public @CoerceToNative Map<ServerConfigurationPacketListener, ServerboundChangeDifficultyPacket> listWorlds(MinecraftServer server) {
        return StreamSupport.stream(server.getWorlds().spliterator(), false)
                .collect(Collectors.toMap(x -> x.getRegistryKey().getValue(), x -> x));
    }
}

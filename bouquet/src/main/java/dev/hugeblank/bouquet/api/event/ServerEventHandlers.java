package dev.hugeblank.bouquet.api.event;

// For all events that use classes that the client doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;

public class ServerEventHandlers {

    public interface ChatMessage {
        void onChatMessage(ServerPlayer player, String message);
    }

    public interface PlayerJoin {
        void onPlayerJoin(ServerPlayer player);
    }

    public interface PlayerQuit {
        void onPlayerQuit(ServerPlayer player);
    }

    public interface PlayerBlockCollision {
        void onPlayerBlockCollision(ServerPlayer player, BlockState state);
    }

    public interface ServerTick {
        void onServerTick(MinecraftServer server);
    }

    public interface ServerStart {
        void onServerStart(MinecraftServer server);
    }

    public interface CommandRegistration {
        void onCommandRegistration(String scriptId, String commandName, boolean successful);
    }
}

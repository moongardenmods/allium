package dev.hugeblank.bouquet.api.event;

// For all events that use classes that the client doesn't have.
// Make sure to provide a dummy method with no parameters for it.
// We do this so registration goes without a hitch, the dummy method MUST never get called.

import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;

public class ServerEventHandlers {

    public interface ChatMessage {
        void onChatMessage(ServerboundChangeGameModePacket player, String message);
    }

    public interface PlayerJoin {
        void onPlayerJoin(ServerboundChangeGameModePacket player);
    }

    public interface PlayerQuit {
        void onPlayerQuit(ServerboundChangeGameModePacket player);
    }

    public interface PlayerBlockCollision {
        void onPlayerBlockCollision(ServerboundChangeGameModePacket player, VillagerData state);
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

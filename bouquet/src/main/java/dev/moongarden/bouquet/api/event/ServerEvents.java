package dev.moongarden.bouquet.api.event;

import dev.moongarden.allium.api.event.SimpleEventType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class ServerEvents {

    // server gets a chat message
    public static final SimpleEventType<ChatMessage> CHAT_MESSAGE = new SimpleEventType<>();
    // player joins the game
    public static final SimpleEventType<PlayerJoin> PLAYER_JOIN = new SimpleEventType<>();
    // player leaves the game
    public static final SimpleEventType<PlayerQuit> PLAYER_QUIT = new SimpleEventType<>();
    // player collides with a block
    public static final SimpleEventType<PlayerBlockCollision> PLAYER_BLOCK_COLLISION = new SimpleEventType<>();
    // the result of a registered command
    public static final SimpleEventType<CommandRegistration> COMMAND_REGISTER = new SimpleEventType<>();
    // server finishes loading
    public static final SimpleEventType<ServerStart> SERVER_START = new SimpleEventType<>();
    // server gets ticked
    public static final SimpleEventType<ServerTick> SERVER_TICK = new SimpleEventType<>();

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

    public interface ServerStart {
        void onServerStart(MinecraftServer server);
    }

    public interface ServerTick {
        void onServerTick(MinecraftServer server);
    }

    public interface CommandRegistration {
        void onCommandRegistration(String scriptId, String commandName, boolean successful);
    }
}

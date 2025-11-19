package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.FileHelper;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@LuaWrapped(name = "config")
public record ConfigLib(Script script) implements WrappedLuaLibrary {

    @LuaWrapped
    public LuaValue load() throws IOException {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getID() + ".json");
        if (Files.exists(path)) {
            return JsonLib.fromJson(Files.readString(path));
        }
        return null;
    }

    @LuaWrapped
    public void save(LuaValue element) throws IOException, LuaError {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getID() + ".json");
        String cfg = JsonLib.PRETTY.toJson(JsonLib.toJsonElement(element));
        Files.deleteIfExists(path);
        OutputStream outputStream = Files.newOutputStream(path);
        outputStream.write(cfg.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }
}

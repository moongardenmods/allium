package dev.hugeblank.bouquet.api.lib.fs;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@LuaWrapped
public class LuaReadHandle extends LuaHandleBase {

    public LuaReadHandle(Script script, Path path) throws IOException {
        super(script, FileChannel.open(path, Set.of(StandardOpenOption.READ, StandardOpenOption.CREATE)));
    }

    @LuaWrapped
    public String read(@OptionalArg Integer count) throws LuaError {
        return readInternal(handle, count);
    }

    @LuaWrapped
    public String readLine(@OptionalArg Boolean withTrailing) throws LuaError {
        return readLineInternal(handle, withTrailing);
    }

    @LuaWrapped
    public String readAll() throws LuaError {
        return readAllInternal(handle);
    }
}

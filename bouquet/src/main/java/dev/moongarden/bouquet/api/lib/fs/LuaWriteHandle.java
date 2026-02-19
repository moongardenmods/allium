package dev.moongarden.bouquet.api.lib.fs;

import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.api.LuaWrapped;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

@LuaWrapped
public class LuaWriteHandle extends LuaHandleBase {

    public LuaWriteHandle(Script script, Path path, boolean append) throws IOException {
        super(script, FileChannel.open(
                path,
                Set.of(append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE))
        );
    }

    @LuaWrapped
    public void write(String out) throws LuaError {
        writeInternal(handle, out);
    }

    @LuaWrapped
    public void writeLine(String out) throws LuaError {
        writeInternal(handle, out+'\n');
    }

    @LuaWrapped
    public void flush() throws LuaError {
        flushInternal(handle);
    }
}

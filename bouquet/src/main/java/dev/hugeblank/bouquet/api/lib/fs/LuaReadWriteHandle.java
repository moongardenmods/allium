package dev.hugeblank.bouquet.api.lib.fs;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class LuaReadWriteHandle extends LuaHandleBase {
    public LuaReadWriteHandle(Script script, Path path, boolean append, boolean truncate) throws IOException {
        super(script, FileChannel.open(path, setOpenOptions(append, truncate)));

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

    private static Set<StandardOpenOption> setOpenOptions(boolean append, boolean truncate) {
        Set<StandardOpenOption> options = new HashSet<>(Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE));
        if (truncate) options.add(StandardOpenOption.TRUNCATE_EXISTING);
        if (append) options.add(StandardOpenOption.APPEND);
        return options;
    }
}

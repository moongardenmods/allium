package dev.moongarden.bouquet.api.lib.fs;

import dev.moongarden.allium.api.ScriptResource;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.nio.channels.FileChannel;

@LuaWrapped
public abstract class LuaHandleBase implements LuaHandle, ScriptResource {

    protected final FileChannel handle;

    public LuaHandleBase(Script script, FileChannel handle) {
        script.registerResource(this);
        this.handle = handle;
    }

    @Override
    @LuaWrapped
    public long seek(@OptionalArg String whence, @OptionalArg Long offset) throws LuaError {
        offset = offset == null ? 0 : offset;
        try {
            switch (whence) {
                case "set" -> handle.position(offset);
                case "end" -> handle.position(handle.size() + offset);
                default -> handle.position(handle.position() + offset);
            }
            return handle.position();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    @Override
    @LuaWrapped
    public void close() throws LuaError {
        try {
            handle.close();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }
}

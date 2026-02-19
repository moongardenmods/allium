package dev.moongarden.bouquet.api.lib.fs;

import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public interface LuaHandle {
    char EOF = (char)-1;

    @SuppressWarnings("unused")
    long seek(String whence, Long offset) throws LuaError;
    @SuppressWarnings("unused")
    void close() throws LuaError;

    default String readInternal(FileChannel handle, @Nullable Integer count) throws LuaError {
        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(count == null ? 1 : count);
            handle.read(buf);
            buf.position(0);
            return StandardCharsets.UTF_8.decode(buf).toString();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    default String readLineInternal(FileChannel handle, @Nullable Boolean withTrailing) throws LuaError {
        StringBuilder builder = new StringBuilder();
        char value;
        ByteBuffer buf = ByteBuffer.allocateDirect(1);
        do {
            try {
                handle.read(buf);
                value = buf.getChar();
                buf.clear();
                if (value == '\n' && withTrailing != null && withTrailing) {
                    builder.append(value);
                } else if (value != '\n') {
                    builder.append(value);
                }
            } catch (IOException e) {
                throw new LuaError(e);
            }
        } while(value != EOF && value != '\n');
        return builder.toString();
    }

    default String readAllInternal(FileChannel handle) throws LuaError {
        StringBuilder builder = new StringBuilder();
        try {
            int iterations = Math.toIntExact((handle.size() / (long) Integer.MAX_VALUE) + 1);
            ByteBuffer buf = ByteBuffer.allocateDirect(handle.size() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) handle.size());
            for (int i = 0; i < iterations; i++) {
                long chunk = handle.size()-handle.position();
                buf.limit(chunk < (long)Integer.MAX_VALUE ? (int)chunk : Integer.MAX_VALUE);
                handle.read(buf);
                buf.position(0);
                builder.append(StandardCharsets.UTF_8.decode(buf));
                buf.clear();
            }
            return builder.toString();
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    default void writeInternal(FileChannel handle, String out) throws LuaError {
        ByteBuffer buf = ByteBuffer.allocateDirect(out.length());
        buf.put(out.getBytes(StandardCharsets.UTF_8));
        buf.position(0);
        try {
            //noinspection ResultOfMethodCallIgnored
            handle.write(buf);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }

    default void flushInternal(FileChannel handle) throws LuaError {
        try {
            handle.force(true);
        } catch (IOException e) {
            throw new LuaError(e);
        }
    }
}

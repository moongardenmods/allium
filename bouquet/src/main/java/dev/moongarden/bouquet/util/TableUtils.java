package dev.moongarden.bouquet.util;

import org.squiddev.cobalt.*;

import java.util.concurrent.atomic.AtomicBoolean;

public class TableUtils {
    public static void forEach(LuaTable table, LuaBiConsumer<LuaValue, LuaValue> consumer) throws LuaError {
        LuaValue key = Constants.NIL;
        while (true) {
            Varargs entry = table.next(key);
            key = entry.arg(1);
            if (key == Constants.NIL) break;
            consumer.accept(key, entry.arg(2));
        }
    }

    public static boolean probablyArray(LuaTable table) throws LuaError {
        LuaValue key = Constants.NIL;
        while (true) {
            Varargs entry = table.next(key);
            key = entry.arg(1);
            if (key == Constants.NIL) break;
            if (!(key instanceof LuaInteger)) return false;
        }
        return true;
    }

    public static boolean assuredlyArray(LuaTable table) throws LuaError {
        AtomicBoolean bool = new AtomicBoolean(true);
        forEach(table, (k, _) -> {
            if (!(k instanceof LuaInteger)) {
                bool.set(false);
            }
        });
        return bool.get();
    }

    // Preserve ordering, stop at nil value
    public static void forEachI(LuaTable table, LuaBiConsumer<Integer, LuaValue> consumer) throws LuaError {
        if (!probablyArray(table)) return;
        int i = 1;
        LuaValue val = table.rawget(i);
        while (val != Constants.NIL) {
            consumer.accept(i, val);
            val = table.rawget(++i);
        }
    }

    public interface LuaBiConsumer<T, U> {
        void accept(T t, U u) throws LuaError;
    }
}

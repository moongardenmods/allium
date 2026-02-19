package dev.moongarden.bouquet.util;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaValue;

import java.util.Spliterator;
import java.util.function.Consumer;

public class TableSpliterator implements Spliterator<LuaValue> {
    private final LuaTable table;
    private LuaValue key = Constants.NIL;

    public TableSpliterator(LuaTable table) {
        this.table = table;
    }

    @Override
    public boolean tryAdvance(Consumer<? super LuaValue> action) {
        try {
            LuaValue nextKey = table.next(key).first();
            if (!nextKey.equals(Constants.NIL)) {
                action.accept(table.rawget(nextKey));
                key = nextKey;
                return true;
            }
        } catch (LuaError ignored) {}
        return false;
    }

    @Override
    public Spliterator<LuaValue> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return table.size();
    }

    @Override
    public long getExactSizeIfKnown() {
        return table.size();
    }

    @Override
    public int characteristics() {
        return 0;
    }
}

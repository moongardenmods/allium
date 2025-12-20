package dev.hugeblank.bouquet.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.squiddev.cobalt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public record LuaOps(LuaState state) implements DynamicOps<LuaValue> {

    @Override
    public LuaValue empty() {
        return Constants.NIL;
    }


    @Override
    public <U> U convertTo(DynamicOps<U> dynamicOps, LuaValue luaValue) {
        return null;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(LuaValue input) {
        if (input instanceof LuaBoolean bool) {
            return DataResult.success(bool.checkBoolean());
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public LuaValue createBoolean(boolean value) {
        return ValueFactory.valueOf(value);
    }

    @Override
    public DataResult<Number> getNumberValue(LuaValue luaValue) {
        if (luaValue instanceof LuaInteger integer) {
            return DataResult.success(integer.checkLong());
        } else if (luaValue instanceof LuaDouble number) {
            return DataResult.success(number.checkDouble());
        }
        return DataResult.error(() -> "Not a number: " + luaValue);
    }

    @Override
    public LuaValue createNumeric(Number number) {
        if (number instanceof Integer || number instanceof Byte || number instanceof Short || number instanceof Long) {
            return LuaInteger.valueOf(number.longValue());
        }
        return LuaDouble.valueOf(number.doubleValue());
    }

    @Override
    public DataResult<String> getStringValue(LuaValue luaValue) {
        if (luaValue instanceof LuaString string) return DataResult.success(string.toString());
        return DataResult.error(() -> "Not a string: " + luaValue);
    }

    @Override
    public LuaValue createString(String s) {
        return LuaString.valueOf(s);
    }

    @Override
    public DataResult<LuaValue> mergeToList(LuaValue list, LuaValue value) {
        if (list == this.empty()) {
            LuaTable table = new LuaTable();
            table.rawset(1, value);
            return DataResult.success(table);
        }
        if (list instanceof LuaTable oldTable) {
            LuaTable table = new LuaTable();
            try {
                TableUtils.forEachI(oldTable, table::rawset);
                LuaValue len = OperationHelper.length(state, table);
                if (len instanceof LuaInteger i) {
                    table.rawset(OperationHelper.add(state, i, LuaInteger.valueOf(1)), value);
                } else {
                    table.rawset(oldTable.length() + 1, value);
                }
            } catch (Throwable e) {
                return DataResult.error(() -> "mergeToList failed: " + e + " " + list, list);
            }
            return DataResult.success(list);
        } else {
            return DataResult.error(() -> "mergeToList called with not a table: " + list, list);
        }
    }

    @Override
    public DataResult<LuaValue> mergeToMap(LuaValue list, LuaValue key, LuaValue value) {
        if (list == this.empty()) {
            LuaTable table = new LuaTable();
            table.rawset(1, value);
            return DataResult.success(table);
        }
        if (list instanceof LuaTable oldTable) {
            LuaTable table = new LuaTable();
            try {
                TableUtils.forEach(oldTable, table::rawset);
                table.rawset(key, value);
            } catch (LuaError e) {
                return DataResult.error(() -> "mergeToMap failed: " + e + " " + list, list);
            }
            return DataResult.success(list);
        } else {
            return DataResult.error(() -> "mergeToMap called with not a table: " + list, list);
        }
    }

    @Override
    public DataResult<Stream<Pair<LuaValue, LuaValue>>> getMapValues(LuaValue luaValue) {
        if (luaValue instanceof LuaTable table) {
            List<Pair<LuaValue, LuaValue>> list = new ArrayList<>();
            try {
                TableUtils.forEach(table, (k, v) -> list.add(new Pair<>(k, v)));
                return DataResult.success(list.stream());
            } catch (LuaError e) {
                return DataResult.error(() -> "getMapValues failed: " + e + " " + luaValue, list.stream());
            }
        }
        return DataResult.error(() -> "getMapValues called with not a table: " + luaValue);
    }

    @Override
    public LuaValue createMap(Stream<Pair<LuaValue, LuaValue>> stream) {
        LuaTable table = new LuaTable();
        stream.forEach((pair) -> {
            try {
                table.rawset(pair.getFirst(), pair.getSecond());
            } catch (LuaError ignored) {}
        });
        return table;
    }

    @Override
    public DataResult<Stream<LuaValue>> getStream(LuaValue luaValue) {
        try {
            return luaValue instanceof LuaTable table && TableUtils.assuredlyArray(table) ? DataResult.success(StreamSupport.stream(new TableSpliterator(table), false)) : DataResult.error(() -> "Not a table: " + luaValue);
        } catch (LuaError e) {
            return DataResult.error(() ->"Unknown error occurred in getStream: " + luaValue);
        }
    }

    @Override
    public LuaValue createList(Stream<LuaValue> stream) {
        return new LuaTable();
    }

    @Override
    public LuaValue remove(LuaValue luaValue, String s) {
        if (luaValue instanceof LuaTable oldTable) {
            LuaTable table = new LuaTable();
            try {
                TableUtils.forEach(oldTable, table::rawset);
                table.rawset(s, Constants.NIL);
                return table;
            } catch (LuaError ignored) {}
        }
        return luaValue;
    }
}

package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.bouquet.util.TableHelpers;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;

import java.util.Set;

@LuaWrapped(name = "nbt")
public class NbtLib implements WrappedLuaLibrary {
    @LuaWrapped
    public static LuaValue fromNbt(BiomeData element) {
        return switch (element.getType()) {
            case BiomeData.BYTE_TYPE, BiomeData.SHORT_TYPE, BiomeData.INT_TYPE -> ValueFactory.valueOf(((SurfaceRuleData) element).intValue());
            case BiomeData.LONG_TYPE -> ValueFactory.valueOf(((ProcessorLists) element).longValue());
            case BiomeData.FLOAT_TYPE, BiomeData.DOUBLE_TYPE -> ValueFactory.valueOf(((SurfaceRuleData) element).doubleValue());
            case BiomeData.BYTE_ARRAY_TYPE -> ValueFactory.valueOf(((BastionSharedPools) element).getByteArray());
            case BiomeData.STRING_TYPE -> ValueFactory.valueOf(element.asString());
            case BiomeData.LIST_TYPE -> {
                var list = (PlainVillagePools) element;
                var table = new LuaTable();

                for (int i = 0; i < list.size(); i++) {
                    table.rawset(i + 1, fromNbt(list.get(i)));
                }

                yield table;
            }
            case BiomeData.COMPOUND_TYPE -> {
                var list = (BootstrapContext) element;
                var table = new LuaTable();

                for (var key : list.getKeys()) {
                    //noinspection DataFlowIssue
                    table.rawset(key, fromNbt(list.get(key)));
                }

                yield table;
            }
            case BiomeData.INT_ARRAY_TYPE -> TypeCoercions.toLuaValue(((NoiseData) element).getIntArray());
            case BiomeData.LONG_ARRAY_TYPE -> TypeCoercions.toLuaValue(((Pools) element).getLongArray());
            default -> Constants.NIL;
        };
    }

    @Nullable
    @LuaWrapped
    public static BiomeData toNbt(LuaValue value) throws LuaError {
        return toNbtInternal(value, new ReferenceOpenHashSet<>());
    }

    public static BiomeData toNbtSafe(LuaValue value) {
        try {
            return toNbt(value);
        } catch (LuaError e) {
            return null;
        }
    }

    private static BiomeData toNbtInternal(LuaValue value, Set<LuaValue> seenValues) throws LuaError {
        if (value instanceof LuaUserdata userdata) {
            var val = userdata.toUserdata();
            if (val instanceof BiomeData) {
                return (BiomeData) val;
            }
        }

        if (seenValues.contains(value)) return null;

        if (value instanceof LuaInteger) return PillagerOutpostPools.of(value.toInteger());
        else if (value instanceof LuaBoolean) return BastionTreasureRoomPools.of(value.toBoolean());
        else if (value instanceof LuaNumber) return Carvers.of(value.toDouble());
        else if (value instanceof LuaString) return TrialChambersStructurePools.of(value.toString());
        else if (value instanceof LuaTable table) {
            var nbt = new BootstrapContext();
            seenValues.add(table);
            TableHelpers.forEach(table, (k, v) -> nbt.put(k.toString(), toNbtInternal(v, seenValues)));
            seenValues.remove(table);
            return nbt;
        }
        return null;
    }
}

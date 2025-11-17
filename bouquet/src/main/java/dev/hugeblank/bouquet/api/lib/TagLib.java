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

@LuaWrapped(name = "tag")
public class TagLib implements WrappedLuaLibrary {
    @LuaWrapped
    public static LuaValue fromTag(Tag element) {
        return switch (element.getId()) {
            case Tag.TAG_BYTE, Tag.TAG_SHORT, Tag.TAG_INT -> ValueFactory.valueOf(((NumericTag) element).intValue());
            case Tag.TAG_LONG -> ValueFactory.valueOf(((LongTag) element).longValue());
            case Tag.TAG_FLOAT, Tag.TAG_DOUBLE -> ValueFactory.valueOf(((NumericTag) element).doubleValue());
            case Tag.TAG_BYTE_ARRAY -> ValueFactory.valueOf(((ByteArrayTag) element).getAsByteArray());
            case Tag.TAG_STRING -> ValueFactory.valueOf(element.asString().orElseThrow());
            case Tag.TAG_LIST -> {
                var list = (ListTag) element;
                var table = new LuaTable();

                for (int i = 0; i < list.size(); i++) {
                    table.rawset(i + 1, fromTag(list.get(i)));
                }

                yield table;
            }
            case Tag.TAG_COMPOUND -> {
                var list = (CompoundTag) element;
                var table = new LuaTable();

                for (var key : list.keySet()) {
                    //noinspection DataFlowIssue
                    table.rawset(key, fromTag(list.get(key)));
                }

                yield table;
            }
            case Tag.TAG_INT_ARRAY -> TypeCoercions.toLuaValue(((IntArrayTag) element).getAsIntArray());
            case Tag.TAG_LONG_ARRAY -> TypeCoercions.toLuaValue(((LongArrayTag) element).getAsLongArray());
            default -> Constants.NIL;
        };
    }

    @Nullable
    @LuaWrapped
    public static Tag toTag(LuaValue value) throws LuaError {
        return toTagInternal(value, new ReferenceOpenHashSet<>());
    }

    @LuaWrapped
    public static Tag toTagSafe(LuaValue value) {
        try {
            return toTag(value);
        } catch (LuaError e) {
            return null;
        }
    }

    private static Tag toTagInternal(LuaValue value, Set<LuaValue> seenValues) throws LuaError {
        if (value instanceof LuaUserdata userdata) {
            var val = userdata.toUserdata();
            if (val instanceof Tag) {
                return (Tag) val;
            }
        }

        if (seenValues.contains(value)) return null;

        if (value instanceof LuaInteger) return IntTag.valueOf(value.toInteger());
        else if (value instanceof LuaBoolean) return ByteTag.valueOf(value.toBoolean());
        else if (value instanceof LuaNumber) return DoubleTag.valueOf(value.toDouble());
        else if (value instanceof LuaString) return StringTag.valueOf(value.toString());
        else if (value instanceof LuaTable table) {
            var nbt = new CompoundTag();
            seenValues.add(table);
            TableHelpers.forEach(table, (k, v) -> {
                Tag nv = toTagInternal(v, seenValues);
                if (nv != null) nbt.put(k.toString(), nv);
            });
            seenValues.remove(table);
            return nbt;
        }
        return null;
    }
}

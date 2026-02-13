package dev.hugeblank.bouquet.api.lib.http;

import com.google.gson.*;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.api.OptionalArg;
import dev.hugeblank.allium.loader.type.userdata.InstanceUserdata;
import dev.hugeblank.bouquet.util.TableUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.squiddev.cobalt.*;

import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.util.Set;

@LuaWrapped
public class LuaByteBuf {
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
    private static final Cleaner CLEANER = Cleaner.create();

    private final ByteBuf raw;
    private Charset preferredCharset;

    public LuaByteBuf(ByteBuf raw, Charset preferredCharset) {
        this.raw = raw;
        this.preferredCharset = preferredCharset;

        CLEANER.register(this, new BufferCleanupAction(raw));
    }

    @LuaWrapped
    public LuaByteBuf writeBoolean(boolean value) {
        raw.writeBoolean(value);
        return this;
    }

    @LuaWrapped
    public boolean readBoolean() {
        return raw.readBoolean();
    }

    @LuaWrapped
    public LuaByteBuf writeByte(int value) {
        raw.writeByte(value);
        return this;
    }

    @LuaWrapped
    public byte readByte() {
        return raw.readByte();
    }

    @LuaWrapped
    public LuaByteBuf writeShort(int value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeShortLE(value);
        else
            raw.writeShort(value);

        return this;
    }

    @LuaWrapped
    public short readShort(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readShortLE();
        else
            return raw.readShort();
    }

    @LuaWrapped
    public LuaByteBuf writeInt(int value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeIntLE(value);
        else
            raw.writeInt(value);

        return this;
    }

    @LuaWrapped
    public int readInt(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readIntLE();
        else
            return raw.readInt();
    }

    @LuaWrapped
    public LuaByteBuf writeLong(long value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeLongLE(value);
        else
            raw.writeLong(value);

        return this;
    }

    @LuaWrapped
    public long readLong(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readLongLE();
        else
            return raw.readLong();
    }

    @LuaWrapped
    public LuaByteBuf writeFloat(float value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeFloatLE(value);
        else
            raw.writeFloat(value);

        return this;
    }

    @LuaWrapped
    public float readFloat(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readFloatLE();
        else
            return raw.readFloat();
    }

    @LuaWrapped
    public LuaByteBuf writeDouble(double value, @OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            raw.writeDoubleLE(value);
        else
            raw.writeDouble(value);

        return this;
    }

    @LuaWrapped
    public double readDouble(@OptionalArg Boolean littleEndian) {
        if (littleEndian == null) littleEndian = false;

        if (littleEndian)
            return raw.readDoubleLE();
        else
            return raw.readDouble();
    }

    @LuaWrapped
    public LuaByteBuf writeBytes(byte[] bytes) {
        raw.writeBytes(bytes);

        return this;
    }

    @LuaWrapped
    public byte[] readBytes(int length) {
        byte[] arr = new byte[length];
        raw.readBytes(arr);
        return arr;
    }

    @LuaWrapped
    public LuaByteBuf writeString(String text, @OptionalArg String charset) {
        raw.writeBytes(text.getBytes(getCharsetFor(charset)));
        return this;
    }

    @LuaWrapped
    public LuaByteBuf writeJson(LuaValue value, @OptionalArg String charset, @OptionalArg Boolean compact) throws LuaError {
        return writeString(toJson(value), charset);
    }

    @LuaWrapped
    public String readString(int binaryLength, @OptionalArg String charset) {
        return raw.readBytes(binaryLength).toString(getCharsetFor(charset));
    }

    @LuaWrapped
    public String asString(@OptionalArg String charset) {
        return raw.toString(getCharsetFor(charset));
    }

    @LuaWrapped
    public LuaValue asJson(@OptionalArg String charset) {
        return fromJson(raw.toString(getCharsetFor(charset)));
    }

    private Charset getCharsetFor(String name) {
        if (name == null) return preferredCharset;
        else return Charset.forName(name);
    }

    @LuaWrapped
    public LuaByteBuf setCharset(String name) {
        this.preferredCharset = Charset.forName(name);
        return this;
    }

    @LuaWrapped
    public String getCharset() {
        return this.preferredCharset.name();
    }

    @LuaWrapped
    public int readableBytes() {
        return raw.readableBytes();
    }

    @LuaWrapped
    public ByteBuf getRaw() {
        return raw;
    }

    private record BufferCleanupAction(ReferenceCounted obj) implements Runnable {
        @Override
        public void run() {
            this.obj.release();
        }
    }

    private static String toJson(LuaValue value) throws LuaError {
        JsonElement element = toJsonElementInternal(value, new ReferenceOpenHashSet<>());
        return COMPACT.toJson(element);
    }

    private static JsonElement toJsonElementInternal(LuaValue value, Set<LuaValue> seenValues) throws LuaError {
        if (seenValues.contains(value)) return JsonNull.INSTANCE;

        if (value instanceof InstanceUserdata<?> userdata && userdata.instanceOf(JsonElement.class)) {
            return userdata.toUserdata(JsonElement.class);
        } else if (value instanceof LuaTable table) {
            if (TableUtils.probablyArray(table)) {
                JsonArray out = new JsonArray();
                seenValues.add(table);
                TableUtils.forEachI(table, (i, v) -> out.add(toJsonElementInternal(v, seenValues)));
                seenValues.remove(table);
                return out;
            } else {
                JsonObject out = new JsonObject();
                seenValues.add(table);
                TableUtils.forEach(table, (k, v) -> {
                    if (!k.isString()) {
                        throw new LuaError("Expected json object key of type 'string', got " + k.typeName());
                    }
                    out.add(k.toString(), toJsonElementInternal(v, seenValues));
                });
                seenValues.remove(table);
                return out;
            }
        } else if (value instanceof LuaBoolean) {
            return new JsonPrimitive(value.toBoolean());
        } else if (value instanceof LuaInteger) {
            return new JsonPrimitive(value.toInteger());
        } else if (value instanceof LuaNumber) {
            return new JsonPrimitive(value.toDouble());
        } else if (value instanceof LuaString) {
            return new JsonPrimitive(value.toString());
        } else if (value instanceof LuaNil) {
            return JsonNull.INSTANCE;
        }
        throw new LuaError("Could not parse value " + value);
    }

    private static LuaValue fromJson(String json) {
        return fromJsonElement(JsonParser.parseString(json));
    }

    private static LuaValue fromJsonElement(JsonElement element) {
        if (element == null) return Constants.NIL;
        if (element.isJsonObject()) {
            LuaTable out = new LuaTable();
            JsonObject object = element.getAsJsonObject();
            object.entrySet().forEach((entry) -> out.rawset(
                    entry.getKey(), fromJsonElement(entry.getValue())
            ));
            return out;
        } else if (element.isJsonArray()) {
            LuaTable out = new LuaTable();
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                out.rawset(i+1, fromJsonElement(array.get(i)));
            }
            return out;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? Constants.TRUE : Constants.FALSE;
            } else if (primitive.isNumber()) {
                return ValueFactory.valueOf(primitive.getAsDouble());
            } else if (primitive.isString()) {
                return ValueFactory.valueOf(primitive.getAsString());
            } else {
                return Constants.NIL;
            }
        } else {
            return Constants.NIL;
        }
    }
}

package dev.moongarden.bouquet.api.lib.http;

import dev.moongarden.allium.api.CoerceToNative;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@LuaWrapped
public class LuaHttpResponse {
    private final HttpResponse<byte[]> raw;
    private final ByteBuf buf;

    public LuaHttpResponse(HttpResponse<byte[]> raw) {
        this.raw = raw;
        this.buf = Unpooled.wrappedBuffer(raw.body());
    }

    @LuaWrapped
    public LuaByteBuf body(@OptionalArg Charset charset) {
        return new LuaByteBuf(buf, charset == null ? StandardCharsets.UTF_8 : charset);
    }

    @LuaWrapped
    public int status() {
        return raw.statusCode();
    }

    @LuaWrapped
    public @CoerceToNative Map<String, @CoerceToNative List<String>> headers() {
        return raw.headers().map();
    }
}

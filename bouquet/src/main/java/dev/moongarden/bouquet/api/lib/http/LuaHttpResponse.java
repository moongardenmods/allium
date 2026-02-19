package dev.moongarden.bouquet.api.lib.http;

import dev.moongarden.allium.api.CoerceToNative;
import dev.moongarden.allium.api.LuaWrapped;
import io.netty.buffer.Unpooled;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@LuaWrapped
public class LuaHttpResponse {
    private final HttpResponse<byte[]> raw;
    @LuaWrapped public final LuaByteBuf body;

    public LuaHttpResponse(HttpResponse<byte[]> raw) {
        this.raw = raw;

        var buf = Unpooled.wrappedBuffer(raw.body());
        this.body = new LuaByteBuf(buf, StandardCharsets.UTF_8); // TODO: figure out what charset to use
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

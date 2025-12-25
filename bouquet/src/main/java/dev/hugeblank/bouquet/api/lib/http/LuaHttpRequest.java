package dev.hugeblank.bouquet.api.lib.http;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@LuaWrapped
public class LuaHttpRequest {
    private final HttpClient client;
    private final HttpRequest.Builder requestBuilder;
    @LuaWrapped public final LuaByteBuf body;

    private CompletableFuture<LuaHttpResponse> sendFuture;

    public LuaHttpRequest(URI uri, HttpClient client) {
        this.body = new LuaByteBuf(Unpooled.buffer(), StandardCharsets.UTF_8);
        this.requestBuilder = HttpRequest.newBuilder(uri);

        this.requestBuilder.header("User-Agent", "Allium/" + Allium.VERSION);

        this.client = client;
    }

    @LuaWrapped
    public LuaHttpRequest method(String method) {
        this.requestBuilder.method(method, new LazyBodyPublisher(() -> HttpRequest.BodyPublishers.ofByteArray(ByteBufUtil.getBytes(body.getRaw()))));
        return this;
    }

    @LuaWrapped
    public LuaHttpRequest header(String key, String value) {
        this.requestBuilder.header(key, value);
        return this;
    }

    @LuaWrapped
    public LuaHttpRequest contentType(String mimeType) {
        return header(HttpHeaderNames.CONTENT_TYPE.toString(), mimeType + "; charset=" + body.getCharset());
    }

    @LuaWrapped
    public CompletableFuture<LuaHttpResponse> send() {
        if (sendFuture != null) return sendFuture;

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(LuaHttpResponse::new);
    }
}

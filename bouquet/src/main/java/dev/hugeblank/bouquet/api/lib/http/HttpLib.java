package dev.hugeblank.bouquet.api.lib.http;

import java.net.URI;
import java.net.http.HttpClient;

public class HttpLib {
    private final HttpClient client;

    public HttpLib() {
        this.client = HttpClient.newBuilder().build();
    }

    public LuaHttpRequest request(String url) {
        return new LuaHttpRequest(URI.create(url), client);
    }
}

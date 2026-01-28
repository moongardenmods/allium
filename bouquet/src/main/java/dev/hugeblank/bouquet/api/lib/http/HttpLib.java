package dev.hugeblank.bouquet.api.lib.http;

import java.net.URI;
import java.net.http.HttpClient;

public class HttpLib {
    public static final HttpLib INSTANCE = new HttpLib();

    private final HttpClient client;

    private HttpLib() {
        this.client = HttpClient.newBuilder().build();
    }

    public LuaHttpRequest request(String url) {
        return new LuaHttpRequest(URI.create(url), client);
    }
}

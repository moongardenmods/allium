package dev.hugeblank.bouquet.api.lib.http;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.hugeblank.allium.api.WrappedLibrary;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;

import java.net.URI;

public class HttpLib implements WrappedLibrary {
    private final EventLoopGroup group;

    public HttpLib() {
        this.group = new MultiThreadIoEventLoopGroup(
                (new ThreadFactoryBuilder()).setNameFormat("[Allium] Netty NIO IO #%d").setDaemon(true).build(),
                NioIoHandler.newFactory()
        );
    }

    public HttpRequest request(String url) {
        return new HttpRequest(URI.create(url), group);
    }
}

package dev.moongarden.bouquet.api.lib.http;

import com.google.common.base.Suppliers;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class LazyBodyPublisher implements HttpRequest.BodyPublisher {
    private final Supplier<HttpRequest.BodyPublisher> factory;

    public LazyBodyPublisher(Supplier<HttpRequest.BodyPublisher> factory) {
        this.factory = Suppliers.memoize(factory::get);
    }

    @Override
    public long contentLength() {
        return factory.get().contentLength();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        factory.get().subscribe(subscriber);
    }
}

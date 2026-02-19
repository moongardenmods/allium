package dev.moongarden.allium.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.util.Map;

// This class was directly inspired by Fabric-ASM. Thank you Chocohead for paving this path for me to walk down with my goofy Lua mod.
// https://github.com/Chocohead/Fabric-ASM/blob/master/src/com/chocohead/mm/CasualStreamHandler.java
public class ByteArrayStreamHandler extends URLStreamHandler {
    private final Map<String, byte[]> providers;

    public ByteArrayStreamHandler(Map<String, byte[]> providers) {
        this.providers = providers;
    }

    public static URL create(String protocol, Map<String, byte[]> providers) {
        try {
            return URL.of(URI.create(protocol + ":/"), new ByteArrayStreamHandler(providers));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected URLConnection openConnection(URL url) {
        String path = url.getPath().substring(1);
        return providers.containsKey(path) ? new ByteArrayStreamConnection(url, providers.get(path)) : null;
    }

    private static final class ByteArrayStreamConnection extends URLConnection {
        private final byte[] bytes;

        public ByteArrayStreamConnection(URL url, byte[] bytes) {
            super(url);
            this.bytes = bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void connect() {
            throw new UnsupportedOperationException();
        }
    }
}

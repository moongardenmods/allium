package dev.hugeblank.allium.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.loader.mixin.MixinClassBuilder;
import dev.hugeblank.allium.loader.mixin.MixinClassInfo;
import dev.hugeblank.allium.util.asm.VisitedClass;
import org.spongepowered.asm.mixin.Mixins;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MixinConfigUtil {
    public static final String MIXIN_PACKAGE = "allium.mixin";
    public static final String MIXIN_CONFIG_NAME = "allium-generated.mixins.json";
    private static boolean complete = false;

    public static boolean isComplete() {
        return complete;
    }

    public static void applyConfiguration() {
        // Create a new mixin config

        Map<String, byte[]> mixinConfigMap = new HashMap<>();

        JsonObject config = JsonBuilder.of()
                .add("required", true)
                .add("compatibilityLevel", "JAVA_21")
                .add("package", "allium.mixin")
                .add("injectors", JsonBuilder.of()
                        .add("defaultRequire", 1)
                        .build()
                )
                .add("mixins", mixinsToJson(MixinClassBuilder.MIXINS, mixinConfigMap))
                .add("client", mixinsToJson(MixinClassBuilder.CLIENT, mixinConfigMap))
                .add("server", mixinsToJson(MixinClassBuilder.SERVER, mixinConfigMap))
                .build();
        String configJson = (new Gson()).toJson(config);
        mixinConfigMap.put(MIXIN_CONFIG_NAME, configJson.getBytes(StandardCharsets.UTF_8));

        URL mixinUrl = ByteArrayStreamHandler.create("allium-mixin", mixinConfigMap);

        // Stuff those files into class loader
        ClassLoader loader = AlliumPreLaunch.class.getClassLoader();
        Method addUrlMethod = null;
        for (Method method : loader.getClass().getMethods()) {
            if (method.getName().equals("addUrlFwd")) {
                addUrlMethod = method;
                break;
            }
        }
        if (addUrlMethod == null) throw new IllegalStateException("Could not find URL loader in ClassLoader " + loader);
        try {
            addUrlMethod.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(addUrlMethod);
            handle.invoke(loader, mixinUrl);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't get handle for " + addUrlMethod, e);
        } catch (Throwable e) {
            throw new RuntimeException("Error invoking URL handler", e);
        }

        Mixins.addConfiguration(MIXIN_CONFIG_NAME);
        VisitedClass.clear();
        complete = true;
    }

    private static JsonArray mixinsToJson(Registry<MixinClassInfo> registry, Map<String, byte[]> configMap) {
        JsonArray mixins = new JsonArray();
        registry.forEach((info) -> {
            String key = info.getID();
            if (key.matches(".*mixin.*")) {
                mixins.add(key.replace(MIXIN_PACKAGE + ".", "").replace(".class", ""));
            }
            configMap.put(key.replace(".", "/").replace("/class", ".class"), info.getBytes());
        });
        return mixins;
    }

    // This class was directly inspired by Fabric-ASM. Thank you Chocohead for paving this path for me to walk down with my goofy Lua mod.
    // https://github.com/Chocohead/Fabric-ASM/blob/master/src/com/chocohead/mm/CasualStreamHandler.java
    public static class ByteArrayStreamHandler extends URLStreamHandler {
        private final Map<String, byte[]> providers;

        public ByteArrayStreamHandler(Map<String, byte[]> providers) {
            this.providers = providers;
        }

        public static URL create(String protocol, Map<String, byte[]> providers) {
            try {
                return new URL(protocol, null, -1, "/", new ByteArrayStreamHandler(providers));
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

    public static class JsonBuilder {
        private final JsonObject object = new JsonObject();

        public JsonBuilder add(String key, String value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonBuilder add(String key, Boolean value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonBuilder add(String key, Number value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonBuilder add(String key, JsonElement value) {
            object.add(key, value);
            return this;
        }

        public JsonObject build() {
            return object;
        }

        public static JsonBuilder of() {
            return new JsonBuilder();
        }
    }
}

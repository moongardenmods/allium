package dev.hugeblank.allium.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.loader.mixin.MixinClassInfo;
import dev.hugeblank.allium.loader.mixin.builder.MixinClassBuilder;
import dev.hugeblank.allium.util.asm.VisitedClass;
import org.spongepowered.asm.mixin.Mixins;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinConfigUtil {
    public static final String MIXIN_PACKAGE = "allium.mixin";
    public static final String MIXIN_CONFIG_NAME = "allium-generated.mixins.json";
    private static boolean complete = false;

    public static boolean isComplete() {
        return complete;
    }

    public static void applyConfiguration() {
        Allium.PROFILER.push("applyConfiguration");
        // Create a new mixin config

        Map<String, byte[]> mixinConfigMap = new HashMap<>();

        JsonObject config = JsonObjectBuilder.of()
                .add("required", true)
                .add("compatibilityLevel", "JAVA_21")
                .add("package", MIXIN_PACKAGE)
                .add("injectors", JsonObjectBuilder.of()
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
        if (addUrlMethod == null) {
            Allium.PROFILER.pop();
            throw new IllegalStateException("Could not find URL loader in ClassLoader " + loader);
        }
        try {
            addUrlMethod.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(addUrlMethod);
            handle.invoke(loader, mixinUrl);
        } catch (IllegalAccessException e) {
            Allium.PROFILER.pop();
            throw new RuntimeException("Couldn't get handle for " + addUrlMethod, e);
        } catch (Throwable e) {
            Allium.PROFILER.pop();
            throw new RuntimeException("Error invoking URL handler", e);
        }

        Mixins.addConfiguration(MIXIN_CONFIG_NAME);
        VisitedClass.clear();
        complete = true;
        Allium.PROFILER.pop();
    }

    private static JsonArray mixinsToJson(List<MixinClassInfo> list, Map<String, byte[]> configMap) {
        JsonArray mixins = new JsonArray();
        list.forEach((info) -> {
            String className = info.className();
            if (className.matches(".*mixin.*")) {
                mixins.add(className.replace(MIXIN_PACKAGE + '.', ""));
            }
            configMap.put(className.replace(".", "/") + ".class", info.classBytes());
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

    public static class JsonObjectBuilder {
        private final JsonObject object = new JsonObject();

        public JsonObjectBuilder add(String key, String value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonObjectBuilder add(String key, Boolean value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonObjectBuilder add(String key, Number value) {
            object.addProperty(key, value);
            return this;
        }

        public JsonObjectBuilder add(String key, JsonElement value) {
            object.add(key, value);
            return this;
        }

        public JsonObject build() {
            return object;
        }

        public static JsonObjectBuilder of() {
            return new JsonObjectBuilder();
        }
    }
}

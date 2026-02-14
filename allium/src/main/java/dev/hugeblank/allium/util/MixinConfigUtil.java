package dev.hugeblank.allium.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.AlliumPreLaunch;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.mixin.builder.MixinClassBuilder;
import dev.hugeblank.allium.util.asm.VisitedClass;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.*;
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
                .add("mixinextras", JsonObjectBuilder.of()
                        .add("minVersion", "0.5.0")
                        .build()
                )
                .add("mixins", MixinLib.mixinsToJson(MixinClassBuilder.MIXINS, mixinConfigMap))
                .add("client", MixinLib.mixinsToJson(MixinClassBuilder.CLIENT, mixinConfigMap))
                .add("server", MixinLib.mixinsToJson(MixinClassBuilder.SERVER, mixinConfigMap))
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

}

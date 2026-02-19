package dev.moongarden.allium.loader.lib;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.moongarden.allium.Allium;
import dev.moongarden.allium.AlliumPreLaunch;
import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.MixinClassInfo;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.MixinMethodAnnotations;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.MixinSugars;
import dev.moongarden.allium.loader.lib.mixin.builder.MixinClassBuilder;
import dev.moongarden.allium.loader.type.StaticBinder;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import dev.moongarden.allium.loader.type.userdata.ClassUserdata;
import dev.moongarden.allium.util.ByteArrayStreamHandler;
import dev.moongarden.allium.util.JsonObjectBuilder;
import dev.moongarden.allium.util.asm.VisitedClass;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixins;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LuaWrapped(name = "mixin")
public class MixinLib extends WrappedScriptLibrary {
    private static boolean COMPLETE = false;

    // This being the way to define embedded "tables" is hilarious to me.
    private static final ClassUserdata<MixinMethodAnnotations> ANNOTATION = StaticBinder.bindClass(EClass.fromJava(MixinMethodAnnotations.class));
    private static final ClassUserdata<MixinSugars> SUGAR = StaticBinder.bindClass(EClass.fromJava(MixinSugars.class));


    @LuaWrapped public final ClassUserdata<MixinMethodAnnotations> annotation = ANNOTATION;
    @LuaWrapped public final ClassUserdata<MixinSugars> sugar = SUGAR;
    private final String mixinPackage = "allium." + script.getID() + ".mixin";
    private final String mixinConfigName = "allium-"+ script.getID() +"-generated.mixins.json";
    private final List<MixinClassInfo> mixins = new ArrayList<>();
    private final List<MixinClassInfo> client = new ArrayList<>();
    private final List<MixinClassInfo> server = new ArrayList<>();
    private final Map<String, String> duckMap = new HashMap<>();
    private final Map<String, MixinMethodHook> eventMap = new HashMap<>();

    private int nextMixinId = 0;

    public MixinLib(Script script) {
        super(script);
    }

    @LuaWrapped
    public MixinMethodHook get(String hookId) {
        return eventMap.get(hookId);
    }

    @LuaWrapped
    public LuaValue quack(String mixinId) throws ClassNotFoundException {
        if (!isComplete())
            throw new IllegalStateException("Duck interface cannot be accessed in pre-launch phase.");
        EClass<?> clazz = EClass.fromJava(Class.forName(duckMap.get(mixinId)));
        return StaticBinder.bindClass(clazz);
    }

    @LuaWrapped
    public MixinClassBuilder to(String targetClass, @OptionalArg @Nullable String[] interfaces, @OptionalArg @Nullable String targetEnvironment, @OptionalArg @Nullable Boolean duck) throws LuaError {
        EnvType targetEnv;
        if (targetEnvironment == null) {
            targetEnv = null;
        } else if (targetEnvironment.equals("client")) {
            targetEnv = EnvType.CLIENT;
        } else if (targetEnvironment.equals("server")) {
            targetEnv = EnvType.SERVER;
        } else {
            throw new LuaError("Mixin for " + targetClass + " expects target environment of nil, 'client' or 'server'.");
        }
        return MixinClassBuilder.create(
                targetClass,
                interfaces == null ? new String[]{} : interfaces,
                targetEnv,
                duck != null && duck,
                script
        );
    }

    public String getUniqueMixinClassName() {
        return mixinPackage.replace('.', '/') + "/GeneratedClass_" + nextMixinId++;
    }

    public void addClassInfo(MixinClassInfo info, EnvType envType) {
        List<MixinClassInfo> envList = (envType == null) ? mixins : switch (envType) {
            case SERVER -> server;
            case CLIENT -> client;
        };
        envList.add(info);
    }

    public boolean hasDuck(String id) {
        return duckMap.containsKey(id);
    }

    public void addDuck(String id, String path) {
        duckMap.put(id, path);
    }

    public void addMethodHook(String id, MixinMethodHook hook) {
        eventMap.put(id, hook);
    }

    public JsonArray mixinsToJson(List<MixinClassInfo> list, Map<String, byte[]> configMap) {
        JsonArray mixins = new JsonArray();
        list.forEach((info) -> {
            String className = info.className();
            if (className.matches(".*mixin.*")) {
                mixins.add(className.replace(mixinPackage + '.', ""));
            }
            configMap.put(className.replace(".", "/") + ".class", info.classBytes());
        });
        return mixins;
    }

    public void applyConfiguration() {
        Allium.PROFILER.push("applyConfiguration");
        // Create a new mixin config

        Map<String, byte[]> mixinConfigMap = new HashMap<>();

        JsonObject config = JsonObjectBuilder.of()
            .add("required", true)
            .add("compatibilityLevel", "JAVA_21")
            .add("package", mixinPackage)
            .add("injectors", JsonObjectBuilder.of()
                .add("defaultRequire", 1)
                .build()
            )
            .add("mixinextras", JsonObjectBuilder.of()
                .add("minVersion", "0.5.0")
                .build()
            )
            .add("mixins", mixinsToJson(mixins, mixinConfigMap))
            .add("client", mixinsToJson(client, mixinConfigMap))
            .add("server", mixinsToJson(server, mixinConfigMap))
            .build();
        String configJson = (new Gson()).toJson(config);
        mixinConfigMap.put(mixinConfigName, configJson.getBytes(StandardCharsets.UTF_8));

        String protocol = script.getID().replaceAll("[^a-zA-Z0-9+\\-.]", "");
        URL mixinUrl = ByteArrayStreamHandler.create("allium-" + protocol + "-mixin", mixinConfigMap);

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

        Mixins.addConfiguration(mixinConfigName);
        VisitedClass.clear();
        Allium.PROFILER.pop();
    }

    public static void setComplete() {
        COMPLETE = true;
    }

    public static boolean isComplete() {
        return COMPLETE;
    }

}

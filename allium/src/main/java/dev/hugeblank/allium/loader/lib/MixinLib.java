package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.event.MixinMethodHook;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.lib.mixin.MixinClassInfo;
import dev.hugeblank.allium.loader.lib.mixin.annotation.method.MixinMethodAnnotations;
import dev.hugeblank.allium.loader.lib.mixin.annotation.sugar.MixinSugars;
import dev.hugeblank.allium.loader.lib.mixin.builder.MixinClassBuilder;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.api.OptionalArg;
import dev.hugeblank.allium.loader.type.userdata.ClassUserdata;
import dev.hugeblank.allium.util.MixinConfigUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LuaWrapped(name = "mixin")
public class MixinLib extends WrappedScriptLibrary {
    // This being the way to define embedded "tables" is hilarious to me.
    private static final ClassUserdata<MixinMethodAnnotations> ANNOTATION = StaticBinder.bindClass(EClass.fromJava(MixinMethodAnnotations.class));
    private static final ClassUserdata<MixinSugars> SUGAR = StaticBinder.bindClass(EClass.fromJava(MixinSugars.class));

    private final List<MixinClassInfo> mixins = new ArrayList<>();
    private final List<MixinClassInfo> client = new ArrayList<>();
    private final List<MixinClassInfo> server = new ArrayList<>();



    private final MixinConfigUtil config = new MixinConfigUtil(script);
    private final Map<String, MixinMethodHook> eventMap = new HashMap<>();
    private final Map<String, String> duckMap = new HashMap<>();
    @LuaWrapped public final ClassUserdata<MixinMethodAnnotations> annotation = ANNOTATION;
    @LuaWrapped public final ClassUserdata<MixinSugars> sugar = SUGAR;

    public MixinLib(Script script) {
        super(script);
    }

    public void registerMixin(MixinClassInfo info, @Nullable EnvType envType) {
        List<MixinClassInfo> envList = (envType == null) ? mixins : switch (envType) {
            case SERVER -> server;
            case CLIENT -> client;
        };

        envList.add(info);
    }

    public void applyConfig() {
        config.apply(mixins, client, server);
    }

    public void addEvent(String id, MixinMethodHook hook) {
        eventMap.put(id, hook);
    }

    // Used in ASM
    public MixinMethodHook getEvent(String id) {
        return eventMap.get(id);
    }

    public void addDuck(String id, String reference) {
        duckMap.put(id, reference);
    }

    @LuaWrapped
    public MixinMethodHook get(String hookId) {
        if (!config.isComplete())
            throw new IllegalStateException("Hook cannot be accessed in pre-launch phase.");
        return eventMap.get(script.getID() + ':' + hookId);
    }

    @LuaWrapped
    public LuaValue quack(String mixinId) throws ClassNotFoundException {
        if (!config.isComplete())
            throw new IllegalStateException("Duck interface cannot be accessed in pre-launch phase.");
        String key = script.getID() + ':' + mixinId;
        if (!duckMap.containsKey(key)) {
            throw new IllegalStateException("No such duck interface with ID '" + key + "'.");
        }
        EClass<?> clazz = EClass.fromJava(Class.forName(duckMap.get(key)));
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
            script,
            config
        );
    }

}

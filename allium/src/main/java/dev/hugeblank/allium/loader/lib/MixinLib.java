package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.event.MixinMethodHook;
import dev.hugeblank.allium.loader.Script;
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

import java.util.HashMap;
import java.util.Map;

@LuaWrapped(name = "mixin")
public class MixinLib extends WrappedScriptLibrary {
    public static final Map<String, MixinMethodHook> EVENT_MAP = new HashMap<>();
    public static final Map<String, String> DUCK_MAP = new HashMap<>();
    // This being the way to define embedded "tables" is hilarious to me.
    private static final ClassUserdata<MixinMethodAnnotations> ANNOTATION = StaticBinder.bindClass(EClass.fromJava(MixinMethodAnnotations.class));
    private static final ClassUserdata<MixinSugars> SUGAR = StaticBinder.bindClass(EClass.fromJava(MixinSugars.class));

    @LuaWrapped public final ClassUserdata<MixinMethodAnnotations> annotation = ANNOTATION;
    @LuaWrapped public final ClassUserdata<MixinSugars> sugar = SUGAR;

    public MixinLib(Script script) {
        super(script);
    }

    @LuaWrapped
    public MixinMethodHook get(String hookId) {
        if (!MixinConfigUtil.isComplete())
            throw new IllegalStateException("Hook cannot be accessed in pre-launch phase.");
        return EVENT_MAP.get(script.getID() + ':' + hookId);
    }

    @LuaWrapped
    public LuaValue quack(String mixinId) throws ClassNotFoundException {
        if (!MixinConfigUtil.isComplete())
            throw new IllegalStateException("Duck interface cannot be accessed in pre-launch phase.");
        EClass<?> clazz = EClass.fromJava(Class.forName(DUCK_MAP.get(script.getID() + ':' + mixinId)));
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

}

package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.WrappedScriptLibrary;
import dev.hugeblank.allium.api.event.MixinMethodHook;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.method.MixinMethodAnnotations;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.MixinSugars;
import dev.hugeblank.allium.loader.mixin.builder.MixinClassBuilder;
import dev.hugeblank.allium.loader.type.AlliumClassUserdata;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;

@LuaWrapped(name = "mixin")
public class MixinLib extends WrappedScriptLibrary {
    // This being the way to define embedded "tables" is hilarious to me.
    private static final AlliumClassUserdata<MixinMethodAnnotations> ANNOTATION = StaticBinder.bindClass(EClass.fromJava(MixinMethodAnnotations.class));
    private static final AlliumClassUserdata<MixinSugars> SUGAR = StaticBinder.bindClass(EClass.fromJava(MixinSugars.class));

    @LuaWrapped public final AlliumClassUserdata<MixinMethodAnnotations> annotation = ANNOTATION;
    @LuaWrapped public final AlliumClassUserdata<MixinSugars> sugar = SUGAR;

    public MixinLib(Script script) {
        super(script);
    }

    @LuaWrapped
    public MixinMethodHook get(String eventId) {
        return MixinMethodHook.EVENT_MAP.get(script.getID() + ':' + eventId);
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

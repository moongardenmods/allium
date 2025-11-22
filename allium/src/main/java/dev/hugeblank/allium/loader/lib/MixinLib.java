package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.MixinClassBuilder;
import dev.hugeblank.allium.loader.mixin.annotation.method.MixinMethodAnnotations;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.MixinSugars;
import dev.hugeblank.allium.loader.type.AlliumClassUserdata;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;

@LuaWrapped(name = "mixin")
public record MixinLib(Script script) implements WrappedLuaLibrary {

    // This being the way to define embedded "tables" is hilarious to me.
    @LuaWrapped(name = "annotation")
    public static final AlliumClassUserdata<MixinMethodAnnotations> ANNOTATION = StaticBinder.bindClass(EClass.fromJava(MixinMethodAnnotations.class));

    @LuaWrapped(name = "sugar")
    public static final AlliumClassUserdata<MixinSugars> SUGAR = StaticBinder.bindClass(EClass.fromJava(MixinSugars.class));

    @LuaWrapped
    public static MixinEventType get(String eventId) {
        return MixinEventType.EVENT_MAP.get(eventId);
    }

    @LuaWrapped
    public static MixinEventType get(String name, String path) {
        return MixinEventType.EVENT_MAP.get(name + ':' + path);
    }

    @LuaWrapped
    public static MixinEventType get(Script script, String path) {
        return MixinEventType.EVENT_MAP.get(script.getID() + ':' + path);
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
        return new MixinClassBuilder(targetClass, interfaces == null ? new String[]{} : interfaces, targetEnv, duck != null && duck, script);
    }

}

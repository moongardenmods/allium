package dev.hugeblank.allium.loader.lib;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.LuaAnnotation;
import dev.hugeblank.allium.loader.mixin.MixinClassBuilder;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

@LuaWrapped(name = "mixin")
public record MixinLib(Script script) implements WrappedLuaLibrary {

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

    @LuaWrapped
    public LuaLocal getLocal(@LuaStateArg LuaState state, String type, @OptionalArg @Nullable LuaTable annotationTable, @OptionalArg @Nullable Boolean mutable) throws InvalidArgumentException, LuaError {
        return new LuaLocal(type, mutable == null || mutable,
                new LuaAnnotation(
                        state,
                        null,
                        annotationTable == null ? new LuaTable() : annotationTable,
                        EClass.fromJava(Local.class)
                )
        );
    }

    public record LuaLocal(String type, boolean mutable, LuaAnnotation luaAnnotation) {}
}

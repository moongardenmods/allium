package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.loader.mixin.annotation.method.injectors.LuaInject;
import dev.hugeblank.allium.loader.mixin.annotation.method.injectors.LuaModifyArg;
import dev.hugeblank.allium.loader.mixin.annotation.method.injectors.LuaModifyArgs;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

@LuaWrapped
public class MixinMethodAnnotations {

    @LuaWrapped
    public static LuaInject inject(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaInject(state, annotation);
    }

    @LuaWrapped
    public static LuaModifyArg modifyArg(@LuaStateArg LuaState state, LuaTable annotation, String targetType) throws InvalidArgumentException, LuaError {
        return new LuaModifyArg(state, annotation, targetType);
    }

    @LuaWrapped
    public static LuaModifyArgs modifyArgs(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaModifyArgs(state, annotation);
    }

}

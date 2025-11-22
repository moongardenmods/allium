package dev.hugeblank.allium.loader.mixin.annotation.sugar;

import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

@LuaWrapped
public class MixinSugars {
    @LuaWrapped
    public static LuaLocal localref(@LuaStateArg LuaState state, String type, @OptionalArg @Nullable LuaTable annotationTable, @OptionalArg @Nullable Boolean mutable) throws InvalidArgumentException, LuaError {
        return new LuaLocal(state, type,
                mutable != null && mutable,
                annotationTable == null ? new LuaTable() : annotationTable);
    }

    @LuaWrapped
    public static LuaShare share(@LuaStateArg LuaState state, String type, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        return new LuaShare(state, type, annotationTable);
    }

    @LuaWrapped
    public static LuaCancellable cancellable(@LuaStateArg LuaState state) throws InvalidArgumentException, LuaError {
        return new LuaCancellable(state);
    }
}

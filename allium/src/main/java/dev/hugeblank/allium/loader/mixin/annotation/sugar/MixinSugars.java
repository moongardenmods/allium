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

    /// Add a variable that exists within the method to the parameter list.
    /// If `mutable` is set, the parameter becomes a corresponding LocalRef.
    /// @see com.llamalad7.mixinextras.sugar.ref
    @LuaWrapped
    public static LuaLocal localref(@LuaStateArg LuaState state, String type, @OptionalArg @Nullable LuaTable annotationTable, @OptionalArg @Nullable Boolean mutable) throws InvalidArgumentException, LuaError {
        return new LuaLocal(state, type,
                mutable != null && mutable,
                annotationTable == null ? new LuaTable() : annotationTable);
    }

    /// Add a shared value to the parameter list.
    /// Shared values are preserved across multiple injections on the same method.
    /// @see com.llamalad7.mixinextras.sugar.Share
    @LuaWrapped
    public static LuaShare share(@LuaStateArg LuaState state, String type, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        return new LuaShare(state, type, annotationTable);
    }

    /// Add a CallbackInfo or CallbackInfoReturnable to the list of parameters.
    /// Useful if one is not already provided.
    /// @see org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    /// @see org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
    /// @see com.llamalad7.mixinextras.sugar.Cancellable
    @LuaWrapped
    public static LuaCancellable cancellable(@LuaStateArg LuaState state) throws InvalidArgumentException, LuaError {
        return new LuaCancellable(state);
    }

    /// Add an exception to the target method's signature.
    /// This sugar does not add a parameter, it is best to add it last to prevent confusion.
    @LuaWrapped(name = "throws")
    public static LuaThrows luaThrows(@LuaStateArg LuaState state, String type) {
        return new LuaThrows(type);
    }
}

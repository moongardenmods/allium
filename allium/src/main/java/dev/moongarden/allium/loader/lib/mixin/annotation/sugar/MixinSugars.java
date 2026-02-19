package dev.moongarden.allium.loader.lib.mixin.annotation.sugar;

import dev.moongarden.allium.api.LuaStateArg;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
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
    public static LuaLocal localref(@LuaStateArg LuaState state, String type, @OptionalArg @Nullable LuaTable annotation, @OptionalArg @Nullable Boolean mutable) throws InvalidArgumentException, LuaError {
        return new LuaLocal(state, type,
                mutable != null && mutable,
                annotation == null ? new LuaTable() : annotation);
    }

    /// Add a shared value to the parameter list.
    /// Shared values are preserved across multiple injections on the same method.
    /// @see com.llamalad7.mixinextras.sugar.Share
    @LuaWrapped
    public static LuaShare share(@LuaStateArg LuaState state, String type, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaShare(state, type, annotation);
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
}

package dev.moongarden.allium.loader.lib.mixin.annotation.sugar;

import com.llamalad7.mixinextras.sugar.Cancellable;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.concurrent.atomic.AtomicBoolean;

public class LuaCancellable extends LuaParameterAnnotation {
    private final AtomicBoolean methodIsReturnable = new AtomicBoolean(false);

    public LuaCancellable(LuaState state) throws InvalidArgumentException, LuaError {
        super(
                state,
                "", // junk type
                new LuaTable(),
                Cancellable.class
        );
    }

    @Override
    public String type() {
        return methodIsReturnable.get() ?
                Type.getDescriptor(CallbackInfoReturnable.class) :
                Type.getDescriptor(CallbackInfo.class);
    }

    public void methodIsReturnable(boolean returnable) {
        methodIsReturnable.set(returnable);
    }
}

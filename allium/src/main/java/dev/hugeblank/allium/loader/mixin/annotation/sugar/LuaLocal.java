package dev.hugeblank.allium.loader.mixin.annotation.sugar;

import com.llamalad7.mixinextras.sugar.Local;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public final class LuaLocal extends LuaAnnotatedParameter {
    private final boolean mutable;

    public LuaLocal(LuaState state, String type, boolean mutable, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, type, annotationTable, Local.class);
        this.mutable = mutable;
    }

    @Override
    public String type() {
        return mutable ? toRefType() : super.type();
    }

}

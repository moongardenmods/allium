package dev.moongarden.allium.loader.lib.mixin.annotation.sugar;

import com.llamalad7.mixinextras.sugar.Share;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public final class LuaShare extends LuaParameterAnnotation {

    public LuaShare(LuaState state, String type, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, type, annotationTable, Share.class);
    }

    @Override
    public String type() {
        return toRefType();
    }
}

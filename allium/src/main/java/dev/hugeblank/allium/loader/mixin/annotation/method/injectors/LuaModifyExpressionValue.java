package dev.hugeblank.allium.loader.mixin.annotation.method.injectors;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class LuaModifyExpressionValue extends ModifyValue {
    public LuaModifyExpressionValue(LuaState state, LuaTable annotationTable, String targetType) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, targetType, ModifyExpressionValue.class);
    }
}

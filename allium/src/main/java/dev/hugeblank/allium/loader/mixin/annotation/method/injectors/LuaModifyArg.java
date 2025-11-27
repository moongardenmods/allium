package dev.hugeblank.allium.loader.mixin.annotation.method.injectors;

import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class LuaModifyArg extends ModifyValue {


    public LuaModifyArg(LuaState state, LuaTable annotationTable, String targetType) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, targetType, ModifyArg.class);
    }
}

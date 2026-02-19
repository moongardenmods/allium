package dev.moongarden.allium.loader.lib.mixin.annotation.method;

import com.llamalad7.mixinextras.expression.Definition;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class LuaDefinition extends LuaMethodAnnotation{
    public LuaDefinition(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, Definition.class);
    }
}

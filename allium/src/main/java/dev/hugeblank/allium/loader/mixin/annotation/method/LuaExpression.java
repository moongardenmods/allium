package dev.hugeblank.allium.loader.mixin.annotation.method;

import com.llamalad7.mixinextras.expression.Expression;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public class LuaExpression extends LuaMethodAnnotation {

    public LuaExpression(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, Expression.class);
    }
}

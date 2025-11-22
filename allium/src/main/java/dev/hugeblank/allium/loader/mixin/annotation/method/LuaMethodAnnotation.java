package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotation;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public abstract class LuaMethodAnnotation {
    protected final LuaAnnotation luaAnnotation;

    public LuaMethodAnnotation(LuaState state, LuaTable annotationTable, Class<?> annotation) throws InvalidArgumentException, LuaError {
        this.luaAnnotation = new LuaAnnotation(
                state,
                null,
                annotationTable,
                EClass.fromJava(annotation)
        );
    }

    public LuaAnnotation luaAnnotation() {
        return luaAnnotation;
    }
}

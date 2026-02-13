package dev.hugeblank.allium.loader.lib.mixin.annotation.method;

import dev.hugeblank.allium.loader.lib.mixin.annotation.LuaAnnotationParser;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

public abstract class LuaMethodAnnotation {
    protected final LuaAnnotationParser parser;

    public LuaMethodAnnotation(LuaState state, LuaTable annotationTable, Class<?> annotation) throws InvalidArgumentException, LuaError {
        this.parser = new LuaAnnotationParser(
                state,
                annotationTable,
                EClass.fromJava(annotation)
        );
    }

    public LuaAnnotationParser parser() {
        return parser;
    }
}

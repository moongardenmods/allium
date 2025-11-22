package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedClass;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.squiddev.cobalt.LuaError;

import java.util.List;

public interface InjectorChef {
    void bake(
            Script script,
            String eventName,
            ClassWriter classWriter,
            VisitedClass mixinClass,
            List<LuaMethodAnnotation> annotations,
            @Nullable List<LuaParameterAnnotation> sugarParameters
    ) throws InvalidMixinException, LuaError, InvalidArgumentException;
}

package dev.hugeblank.allium.loader.mixin.annotation.method.injectors;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaInjectorAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaSugar;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedClass;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

public class LuaWrapMethod extends LuaInjectorAnnotation {
    public LuaWrapMethod(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, WrapMethod.class);
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {

    }
}

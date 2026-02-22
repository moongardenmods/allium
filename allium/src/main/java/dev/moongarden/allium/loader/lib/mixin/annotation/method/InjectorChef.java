package dev.moongarden.allium.loader.lib.mixin.annotation.method;

import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.VisitedClass;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.squiddev.cobalt.LuaError;

import java.util.List;

public interface InjectorChef {
    MixinMethodHook bake(
            Script script,
            String classId,
            String eventId,
            ClassWriter classWriter,
            VisitedClass mixinClass,
            List<LuaMethodAnnotation> annotations,
            @Nullable List<? extends LuaSugar> sugarParameters
    ) throws InvalidMixinException, LuaError;
}

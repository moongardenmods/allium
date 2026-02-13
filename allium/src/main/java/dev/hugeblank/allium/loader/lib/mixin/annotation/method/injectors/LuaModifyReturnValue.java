package dev.hugeblank.allium.loader.lib.mixin.annotation.method.injectors;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.lib.mixin.annotation.method.LuaInjectorAnnotation;
import dev.hugeblank.allium.loader.lib.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.hugeblank.allium.loader.lib.mixin.builder.MixinMethodBuilder;
import dev.hugeblank.allium.loader.lib.mixin.builder.MixinParameter;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class LuaModifyReturnValue extends LuaInjectorAnnotation {
    public LuaModifyReturnValue(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, ModifyReturnValue.class);
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, parser);

        Type target = Type.getReturnType(visitedMethod.descriptor());

        MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                classWriter, visitedMethod,
                createInjectName(script.getID(), visitedMethod.name()),
                List.of(new MixinParameter(target))
        );

        if (sugarParameters != null) methodBuilder.sugars(sugarParameters);

        methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(target)
                .annotations(List.of(parser))
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(eventId))
                .build(script, eventId);
    }
}

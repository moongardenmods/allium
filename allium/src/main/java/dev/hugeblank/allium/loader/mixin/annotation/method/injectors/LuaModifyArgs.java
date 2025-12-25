package dev.hugeblank.allium.loader.mixin.annotation.method.injectors;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaInjectorAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaSugar;
import dev.hugeblank.allium.loader.mixin.builder.MixinMethodBuilder;
import dev.hugeblank.allium.loader.mixin.builder.MixinParameter;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class LuaModifyArgs extends LuaInjectorAnnotation {
    public LuaModifyArgs(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, ModifyArgs.class);
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, parser);

        MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                classWriter, visitedMethod,
                createInjectName(script.getID(), visitedMethod.name()),
                List.of(new MixinParameter(Type.getType(Args.class)))
        );

        if (sugarParameters != null) methodBuilder.sugars(sugarParameters);

        methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(Type.VOID_TYPE)
                .annotations(List.of(parser))
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(eventId))
                .build(script, eventId);
    }
}

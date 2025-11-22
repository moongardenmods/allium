package dev.hugeblank.allium.loader.mixin.annotation.method.injectors;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.MixinMethodBuilder;
import dev.hugeblank.allium.loader.mixin.MixinParameter;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaInjectorAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class LuaModifyArg extends LuaInjectorAnnotation {
    private final String targetType;

    public LuaModifyArg(LuaState state, LuaTable annotationTable, String targetType) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, ModifyArg.class);
        this.targetType = targetType;
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<LuaParameterAnnotation> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, luaAnnotation);

        Type target = Type.getType(targetType);

        MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                classWriter, visitedMethod,
                createInjectName(script.getID(), visitedMethod.name()),
                List.of(new MixinParameter(target))
        );

        if (sugarParameters != null) methodBuilder.luaParameters(sugarParameters);

        MixinMethodBuilder.InvocationReference invocationReference = methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(target)
                .annotations(List.of(luaAnnotation))
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(eventId))
                .build();

        invocationReference.createEvent(eventId);
    }
}

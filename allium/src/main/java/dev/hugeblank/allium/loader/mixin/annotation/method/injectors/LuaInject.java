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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class LuaInject extends LuaInjectorAnnotation {

    public LuaInject(LuaState state, LuaTable annotationTable) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, Inject.class);
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<LuaParameterAnnotation> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, luaAnnotation);
        List<MixinParameter> params = new ArrayList<>(visitedMethod.getParams().stream().map(MixinParameter::new).toList());
        Type returnType = Type.getReturnType(visitedMethod.descriptor());
        if (returnType.equals(Type.VOID_TYPE)) {
            params.add(new MixinParameter(Type.getType(CallbackInfo.class)));
        } else {
            params.add(new MixinParameter(Type.getType(CallbackInfoReturnable.class)));
        }


        MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                classWriter, visitedMethod, createInjectName(script.getID(), visitedMethod.name()), params
        );

        if (sugarParameters != null) methodBuilder.luaParameters(sugarParameters);

        MixinMethodBuilder.InvocationReference invocationReference = methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(Type.VOID_TYPE)
                .annotations(annotations.stream().map(LuaMethodAnnotation::luaAnnotation).toList())
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(eventId))
                .build();

        invocationReference.createEvent(eventId);
    }
}

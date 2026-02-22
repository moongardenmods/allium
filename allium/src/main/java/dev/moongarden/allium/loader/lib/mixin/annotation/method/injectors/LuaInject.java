package dev.moongarden.allium.loader.lib.mixin.annotation.method.injectors;

import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.LuaInjectorAnnotation;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.LuaMethodAnnotation;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.moongarden.allium.loader.lib.mixin.builder.InternalMixinMethodBuilder;
import dev.moongarden.allium.loader.lib.mixin.builder.MixinParameter;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.VisitedClass;
import dev.moongarden.allium.util.asm.VisitedMethod;
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
    public MixinMethodHook bake(Script script, String classId, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, parser);
        List<MixinParameter> params = new ArrayList<>(visitedMethod.getParams().stream().map(MixinParameter::new).toList());
        Type returnType = Type.getReturnType(visitedMethod.descriptor());
        if (returnType.equals(Type.VOID_TYPE)) {
            params.add(new MixinParameter(Type.getType(CallbackInfo.class)));
        } else {
            params.add(new MixinParameter(Type.getType(CallbackInfoReturnable.class)));
        }


        InternalMixinMethodBuilder methodBuilder = new InternalMixinMethodBuilder(
                classWriter, visitedMethod, createInjectName(script.getID(), visitedMethod.name()), params
        );

        if (sugarParameters != null) methodBuilder.sugars(sugarParameters);

        return methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(Type.VOID_TYPE)
                .annotations(annotations.stream().map(LuaMethodAnnotation::parser).toList())
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(script, classId, eventId))
                .buildForClass(script, eventId);
    }
}

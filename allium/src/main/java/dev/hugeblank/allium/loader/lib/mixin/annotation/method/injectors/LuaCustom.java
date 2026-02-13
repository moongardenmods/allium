package dev.hugeblank.allium.loader.lib.mixin.annotation.method.injectors;

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

public class LuaCustom extends LuaInjectorAnnotation {
    private final String methodDescriptor;
    private final List<String> parameters;
    private final String returnType;

    public LuaCustom(LuaState state, LuaTable annotationTable, Class<?> annotation, String methodDescriptor, List<String> parameters, String returnType) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, annotation);
        this.methodDescriptor = methodDescriptor;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    @Override
    public void bake(Script script, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError, InvalidArgumentException {
        VisitedMethod visitedMethod = mixinClass.getMethod(methodDescriptor);
        List<MixinParameter> params = parameters.stream().map(str -> new MixinParameter(Type.getType(str))).toList();

        MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(
                classWriter, visitedMethod, createInjectName(script.getID(), visitedMethod.name()), params
        );

        if (sugarParameters != null) methodBuilder.sugars(sugarParameters);

        methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(Type.getType(returnType))
                .annotations(annotations.stream().map(LuaMethodAnnotation::parser).toList())
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(eventId))
                .build(script, eventId);
    }
}

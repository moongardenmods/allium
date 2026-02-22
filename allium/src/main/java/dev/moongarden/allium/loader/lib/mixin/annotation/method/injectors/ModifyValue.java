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
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ModifyValue extends LuaInjectorAnnotation {
    protected final String targetType;

    public ModifyValue(LuaState state, LuaTable annotationTable, String targetType, Class<?> annotation) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, annotation);
        this.targetType = targetType;
    }

    @Override
    public MixinMethodHook bake(Script script, String classId, String eventId, ClassWriter classWriter, VisitedClass mixinClass, List<LuaMethodAnnotation> annotations, @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, LuaError {
        VisitedMethod visitedMethod = getVisitedMethod(mixinClass, parser);

        Type target = Type.getType(targetType);

        InternalMixinMethodBuilder methodBuilder = new InternalMixinMethodBuilder(
                classWriter, visitedMethod,
                createInjectName(script.getID(), visitedMethod.name()),
                List.of(new MixinParameter(target))
        );

        if (sugarParameters != null) methodBuilder.sugars(sugarParameters);

        return methodBuilder
                .access(visitedMethod.access() & ~(ACC_PUBLIC | ACC_PROTECTED) | ACC_PRIVATE)
                .returnType(target)
                .annotations(List.of(parser))
                .signature(visitedMethod.signature())
                .exceptions(visitedMethod.exceptions())
                .code(createInjectWriteFactory(script, classId, eventId))
                .buildForClass(script, eventId);
    }
}

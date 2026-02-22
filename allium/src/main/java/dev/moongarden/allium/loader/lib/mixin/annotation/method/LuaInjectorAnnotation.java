package dev.moongarden.allium.loader.lib.mixin.annotation.method;

import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.LuaAnnotationParser;
import dev.moongarden.allium.loader.lib.mixin.builder.AbstractMixinBuilder;
import dev.moongarden.allium.loader.lib.mixin.builder.InternalMixinMethodBuilder;
import dev.moongarden.allium.loader.lib.mixin.builder.MixinParameter;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.AsmUtil;
import dev.moongarden.allium.util.asm.Owners;
import dev.moongarden.allium.util.asm.VisitedClass;
import dev.moongarden.allium.util.asm.VisitedMethod;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public abstract class LuaInjectorAnnotation extends LuaMethodAnnotation implements InjectorChef {
    private int methodIndex = 0;

    public LuaInjectorAnnotation(LuaState state, LuaTable annotationTable, Class<?> annotation) throws InvalidArgumentException, LuaError {
        super(state, annotationTable, annotation);
    }

    protected String createInjectName(String scriptId, String visitedMethodName) {
        return scriptId+ "$" +
                visitedMethodName
                        .replace("<", "")
                        .replace(">", "") +
                methodIndex++;
    }

    protected static VisitedMethod getVisitedMethod(VisitedClass mixinClass, LuaAnnotationParser annotation) throws InvalidMixinException, LuaError {
        String descriptor = AbstractMixinBuilder.cleanDescriptor(
            mixinClass,
            annotation.findElement("method", String.class)
        );
        if (!mixinClass.containsMethod(descriptor))
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        return mixinClass.getMethod(descriptor);
    }

    protected static InternalMixinMethodBuilder.WriteFactory createInjectWriteFactory(Script script, String classId, String eventName) {
        final Type objectType = Type.getType(Object.class);

        return (methodVisitor, desc, paramTypes) -> {
            int varPrefix = paramTypes.size();
            Type returnType = Type.getReturnType(desc);
            List<Type> types = paramTypes.stream().map(MixinParameter::getType).toList();

            AsmUtil.getScript(methodVisitor, script);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Owners.SCRIPT, "getMixinLib", "()Ldev/moongarden/allium/loader/lib/MixinLib;", false);
            methodVisitor.visitLdcInsn(classId);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Owners.MIXIN_LIB, "get", "(Ljava/lang/String;)Ldev/moongarden/allium/loader/lib/mixin/builder/HookDefinition;", false);
            methodVisitor.visitLdcInsn(eventName);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Owners.HOOK_DEFINITION, "forId", "(Ljava/lang/String;)Ldev/moongarden/allium/api/event/MixinMethodHook;", false);

            AsmUtil.createArray(methodVisitor, varPrefix, types, Object.class, (visitor, index, arg) -> {
                visitor.visitVarInsn(arg.getOpcode(ILOAD), index); // <- 2
                AsmUtil.wrapPrimitive(visitor, arg); // <- 2 | -> 2 (sometimes)
                if (index == 0) {
                    visitor.visitTypeInsn(CHECKCAST, Owners.OBJECT); // <- 2 | -> 2
                }
            }); // <- 1
            methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    Type.getInternalName(MixinMethodHook.class),
                    "invoke",
                    Type.getMethodDescriptor(objectType, Type.getType(Object[].class)),
                    false
            ); // <- 0 (sometimes) | -> 0, 1
            if (!returnType.equals(Type.VOID_TYPE))
                methodVisitor.visitTypeInsn(CHECKCAST, returnType.getInternalName()); // <- 0 | -> 0
            methodVisitor.visitInsn(returnType.getOpcode(IRETURN));
            methodVisitor.visitMaxs(0, 0);
        };
    }

}

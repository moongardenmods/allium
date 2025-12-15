package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.api.event.MixinMethodHook;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotationParser;
import dev.hugeblank.allium.loader.mixin.builder.MixinMethodBuilder;
import dev.hugeblank.allium.loader.mixin.builder.MixinParameter;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public abstract class LuaInjectorAnnotation extends LuaMethodAnnotation implements InjectorChef{
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
        String descriptor = annotation.findElement("method", String.class);
        // TODO: if the descriptor starts with the class name remove it!
        if (!mixinClass.containsMethod(descriptor))
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        return mixinClass.getMethod(descriptor);
    }

    protected static MixinMethodBuilder.WriteFactory createInjectWriteFactory(String eventName) {
        final Type objectType = Type.getType(Object.class);
        return (classWriter, methodVisitor, desc, paramTypes) -> {
            int varPrefix = paramTypes.size();
            Type returnType = Type.getReturnType(desc);
            List<Type> types = paramTypes.stream().map(MixinParameter::getType).toList();
            methodVisitor.visitFieldInsn(
                    GETSTATIC, Type.getInternalName(MixinMethodHook.class),
                    "EVENT_MAP", Type.getDescriptor(Map.class)
            ); // <- 0
            methodVisitor.visitLdcInsn(eventName); // <- 1
            methodVisitor.visitMethodInsn(
                    INVOKEINTERFACE,
                    Type.getInternalName(Map.class),
                    "get",
                    Type.getMethodDescriptor(objectType, objectType),
                    true
            ); // -> 0, 1 | <- 0
            methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(MixinMethodHook.class)); // <- 0 | -> 0
            AsmUtil.createArray(methodVisitor, varPrefix, types, Object.class, (visitor, index, arg) -> {
                visitor.visitVarInsn(arg.getOpcode(ILOAD), index); // <- 2
                AsmUtil.wrapPrimitive(visitor, arg); // <- 2 | -> 2 (sometimes)
                if (index == 0) {
                    visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
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

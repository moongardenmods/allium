package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.mixin.MixinParameter;
import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotation;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import org.objectweb.asm.MethodVisitor;
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

    protected VisitedMethod getVisitedMethod(VisitedClass mixinClass, LuaAnnotation annotation) throws InvalidMixinException, LuaError {
        String descriptor = annotation.findElement("method", String.class);
        if (!mixinClass.containsMethod(descriptor))
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_DESCRIPTOR, descriptor);
        return mixinClass.getMethod(descriptor);
    }

    protected MethodWriteFactory createInjectWriteFactory(String eventName) {
        return (methodVisitor, desc, paramTypes, access) -> {
            int varPrefix = paramTypes.size();
            List<Type> types = paramTypes.stream().map(MixinParameter::getType).toList();
            AsmUtil.createArray(methodVisitor, varPrefix, types, Object.class, (visitor, index, arg) -> {
                visitor.visitVarInsn(arg.getOpcode(ILOAD), index); // <- 2
                AsmUtil.wrapPrimitive(visitor, arg); // <- 2 | -> 2 (sometimes)
                if (index == 0) {
                    visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(Object.class)); // <- 2 | -> 2
                }
            }); // <- 0
            methodVisitor.visitFieldInsn(
                    GETSTATIC, Type.getInternalName(MixinEventType.class),
                    "EVENT_MAP", Type.getDescriptor(Map.class)
            ); // <- 1
            methodVisitor.visitLdcInsn(eventName); // <- 2
            methodVisitor.visitMethodInsn(
                    INVOKEINTERFACE,
                    Type.getInternalName(Map.class),
                    "get",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)),
                    true
            ); // -> 1, 2 | <- 1
            methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(MixinEventType.class)); // <- 1 | -> 1
            methodVisitor.visitInsn(SWAP); // 0 <-> 1
            methodVisitor.visitMethodInsn(
                    INVOKEVIRTUAL,
                    Type.getInternalName(MixinEventType.class),
                    "invoke",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object[].class)),
                    false
            ); // -> 0, 1

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(0, 0);
        };
    }

    protected String createInjectName(String scriptId, String visitedMethodName) {
        return scriptId+ "$" +
                visitedMethodName
                        .replace("<", "")
                        .replace(">", "") +
                methodIndex++;
    }

    @FunctionalInterface
    public interface MethodWriteFactory {
        void write(MethodVisitor methodVisitor, String descriptor, List<MixinParameter> parameters, int access);
    }
}

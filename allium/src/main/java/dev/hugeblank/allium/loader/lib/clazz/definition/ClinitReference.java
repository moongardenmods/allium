package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.loader.lib.clazz.builder.MethodBuilder;
import dev.hugeblank.allium.loader.type.property.MemberFilter;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import org.lwjgl.openal.AL;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ClinitReference extends ExecutableReference {
    private final List<LuaFunction> functions = new ArrayList<>();
    public ClinitReference() {
        super("<clinit>", null, new WrappedType[]{}, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), Opcodes.ACC_STATIC, true);
        this.function = new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState luaState, Varargs varargs) throws LuaError, UnwindThrowable {
                for (LuaFunction function : functions) {
                    Dispatch.invoke(luaState, function, varargs);
                }
                return Constants.NIL;
            }
        };
    }

    protected void addFunction(LuaFunction func) {
        functions.add(func);
    }

    @Override
    public String getTypeName() {
        return "class initializer";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected void prepareOffsets(WriteContext ctx) {
        ctx.setArrayPos((Type.getArgumentsAndReturnSizes(ctx.descriptor()) >> 2)-1);
        ctx.setParamOffset(1);
    }

    @Override
    protected void writeBeforeHandler(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();

        m.visitLdcInsn(Type.getType('L' + ctx.className() + ';')); // classType
        m.visitMethodInsn(INVOKESTATIC, Owners.INTERNAL_FIELD_BUILDER, "apply", "(Ljava/lang/Class;)V", false); //
        m.visitTypeInsn(NEW, Owners.CLASS_FINAL_FIELD_HOLDER); // fieldHolder
        m.visitLdcInsn(Type.getType('L' + ctx.className() + ';')); // fieldHolder, classType
        m.visitMethodInsn(INVOKESPECIAL, Owners.CLASS_FINAL_FIELD_HOLDER,"<init>", "(Ljava/lang/Class;)V", false); //
    }

    @Override
    protected void writeInitialElements(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        m.visitVarInsn(ALOAD, ctx.arrayPos()); // array
        m.visitLdcInsn(0); // array, 0
        m.visitLdcInsn(Type.getType("L" + ctx.className() + ";")); // array, 0, classType
        m.visitMethodInsn(INVOKESTATIC, Owners.ECLASS, "fromJava", "(Ljava/lang/Class;)Lme/basiqueevangelist/enhancedreflection/api/EClass;", true); // array, 0, eClass
        m.visitFieldInsn(GETSTATIC, Owners.MEMBER_FILTER, "ALL_STATIC_MEMBERS", Type.getDescriptor(MemberFilter.class)); // array, 0, eClass, memberFilter
        m.visitMethodInsn(INVOKESTATIC, Owners.STATIC_BINDER, "bindClass", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;Ldev/hugeblank/allium/loader/type/property/MemberFilter;)Ldev/hugeblank/allium/loader/type/userdata/ClassUserdata;", false); // array, 0, luaClass
        m.visitInsn(AASTORE); //
    }

    @Override
    protected void writeReturn(WriteContext ctx) {
        if (definesFields) {
            MethodVisitor m = ctx.visitor();
            for (ClassBuilder.FieldReference reference : ctx.classFields()) {
                Class<?> rawType = reference.type().raw();

                m.visitLdcInsn(reference.name()); // name
                m.visitLdcInsn(Type.getType('L' + ctx.className() + ';')); // name, classType
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "getField", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false); // fieldValue
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.type().wrapPrimitive().raw())); // cast
                if (rawType.isPrimitive()) AsmUtil.unwrapPrimitive(m, Type.getType(rawType)); // <-> realFieldValue
                m.visitFieldInsn(PUTSTATIC, ctx.className(), reference.name(), Type.getDescriptor(rawType)); // -> realFieldValue
            }
        }
        super.writeReturn(ctx);
    }
}

package dev.moongarden.allium.loader.lib.clazz.definition;

import dev.moongarden.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.moongarden.allium.loader.lib.clazz.builder.MethodBuilder;
import dev.moongarden.allium.loader.type.property.MemberFilter;
import dev.moongarden.allium.util.asm.AsmUtil;
import dev.moongarden.allium.util.asm.Owners;
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
    private final boolean initHolder;

    public ClinitReference(boolean initHolder) {
        super("<clinit>", null, new WrappedType[]{}, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), Opcodes.ACC_STATIC, true);
        this.initHolder = initHolder;
    }

    protected void addFunction(LuaFunction func) {
        if (this.function == null) {
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
        ctx.setArrayPos((Type.getArgumentsAndReturnSizes(ctx.descriptor()) >> 2));
        ctx.setParamOffset(1);
    }

    @Override
    protected void writeBeforeHandler(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();

        m.visitLdcInsn(Type.getType('L' + ctx.className() + ';')); // classType
        m.visitMethodInsn(INVOKESTATIC, Owners.INTERNAL_FIELD_BUILDER, "apply", "(Ljava/lang/Class;)V", false); //
        if (initHolder) {
            m.visitTypeInsn(NEW, Owners.CLASS_FINAL_FIELD_HOLDER); // fieldHolder
            m.visitInsn(DUP); // fieldHolder, fieldHolder
            m.visitLdcInsn(Type.getType('L' + ctx.className() + ';')); // fieldHolder, fieldHolder, classType
            m.visitMethodInsn(INVOKESPECIAL, Owners.CLASS_FINAL_FIELD_HOLDER,"<init>", "(Ljava/lang/Class;)V", false); // fieldHolder
            m.visitVarInsn(ASTORE, ctx.arrayPos()-1); //
        }
    }

    @Override
    protected void writeInitialElements(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        m.visitVarInsn(ALOAD, ctx.arrayPos()); // array
        m.visitLdcInsn(0); // array, 0
        m.visitLdcInsn(Type.getType("L" + ctx.className() + ";")); // array, 0, classType
        m.visitMethodInsn(INVOKESTATIC, Owners.ECLASS, "fromJava", "(Ljava/lang/Class;)Lme/basiqueevangelist/enhancedreflection/api/EClass;", true); // array, 0, eClass
        m.visitFieldInsn(GETSTATIC, Owners.MEMBER_FILTER, "ALL_STATIC_MEMBERS", Type.getDescriptor(MemberFilter.class)); // array, 0, eClass, memberFilter
        m.visitMethodInsn(INVOKESTATIC, Owners.STATIC_BINDER, "bindClass", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;Ldev/moongarden/allium/loader/type/property/MemberFilter;)Ldev/moongarden/allium/loader/type/userdata/ClassUserdata;", false); // array, 0, luaClass
        m.visitInsn(AASTORE); //
    }

    @Override
    protected void writeReturn(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        if (definesFields) {
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

        if (initHolder) {
            m.visitVarInsn(ALOAD, ctx.arrayPos()-1); // fieldHolder
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.CLASS_FINAL_FIELD_HOLDER, "close", "()V", false); //
        }

        super.writeReturn(ctx);
    }
}

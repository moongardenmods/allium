package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.loader.lib.clazz.builder.MethodBuilder;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class ClinitDefinition extends MethodDefinition {
    public ClinitDefinition() {
        super("<clinit>", new WrappedType[]{}, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), Opcodes.ACC_STATIC);
    }

    @Override
    public void write(ClassBuilder.BuilderContext bctx) {
        WriteContext ctx = createContext(bctx);

        ctx.setArrayPos((Type.getArgumentsAndReturnSizes(ctx.descriptor()) >> 2)-1);
        ctx.setParamOffset(1);

        if (definesHandler()) {
            writeStart(ctx);
            writeFirstElement(ctx);
            writeHook(ctx);
            writeEnd(ctx);
        }
    }

    @Override
    protected void writeFirstElement(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        m.visitVarInsn(ALOAD, ctx.arrayPos()); // array
        m.visitLdcInsn(0); // array, 0
        m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "initClassFieldHolder", "(Ljava/lang/String;)Ldev/hugeblank/allium/loader/lib/builder/ClassBuilder$FieldHolder;", false); // array, 0, holder
        m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;)Lorg/squiddev/cobalt/LuaValue;", false); // array, 0, luaValue
        m.visitInsn(AASTORE); //
    }

    @Override
    protected void writeEnd(WriteContext ctx) {
        if (definesFields) {
            MethodVisitor m = ctx.visitor();
            for (ClassBuilder.FieldReference reference : ctx.classFields()) {
                Class<?> rawType = reference.type().raw();

                m.visitLdcInsn(reference.name()); // name
                m.visitLdcInsn(ctx.className()); // name <- className
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "getField", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false); // -> name, className <- fieldValue
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.type().wrapPrimitive().raw())); // cast
                if (rawType.isPrimitive()) AsmUtil.unwrapPrimitive(m, Type.getType(rawType)); // <-> realFieldValue
                m.visitFieldInsn(PUTSTATIC, ctx.className(), reference.name(), Type.getDescriptor(rawType)); // -> realFieldValue
            }

        }
        super.writeEnd(ctx);
    }
}

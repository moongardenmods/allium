package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.loader.lib.clazz.builder.InternalFieldBuilder;
import dev.hugeblank.allium.loader.lib.clazz.builder.MethodBuilder;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.SWAP;

@LuaWrapped
public class ConstructorReference extends ExecutableReference {
    protected final LuaFunction remapper;

    public ConstructorReference(LuaFunction remapper, String index, WrappedType[] params, int access, boolean definesFields) {
        super("<init>", index == null ? "constructor" : index, params, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), access, definesFields);
        this.remapper = remapper;
    }

    @Override
    public boolean isValid() {
        return !definesFields || function != null;
    }

    @Override
    public String getTypeName() {
        return "constructor";
    }

    public void applyRemap(WriteContext ctx) {
        // No need for remap if this() or super() are not invoked.
    }

    @Override
    protected void writeBeforeHandler(WriteContext ctx) {
        applyRemap(ctx);

        MethodVisitor m = ctx.visitor();

        m.visitVarInsn(ALOAD, 0);
        if (definesFields) m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "initInstanceFieldHolder", "(Ljava/lang/Object;)V", false);
    }

    @Override
    protected void writeReturn(WriteContext ctx) {
        if (definesFields) { // Net 0 effect on stack
            MethodVisitor m = ctx.visitor();
            for (ClassBuilder.FieldReference reference : ctx.instanceFields()) {
                Class<?> rawType = reference.type().raw();

                m.visitLdcInsn(reference.name()); // name
                m.visitVarInsn(ALOAD, 0); // name <- this
                m.visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "getField", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", false); // -> name, this <- fieldValue
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.type().wrapPrimitive().raw())); // cast
                if (rawType.isPrimitive()) AsmUtil.unwrapPrimitive(m, Type.getType(rawType)); // <-> realFieldValue
                m.visitVarInsn(ALOAD, 0); // realFieldValue <- this
                m.visitInsn(SWAP); // this, realFieldValue
                m.visitFieldInsn(PUTFIELD, ctx.className(), reference.name(), Type.getDescriptor(reference.type().raw())); // -> this, realFieldValue
            }
            ctx.visitor().visitVarInsn(ALOAD, 0);
            ctx.visitor().visitMethodInsn(INVOKESTATIC, Owners.CLASS_BUILDER, "removeInstanceFieldHolder", "(Ljava/lang/Object;)V", false);
        }
        super.writeReturn(ctx);
    }

    // Only execute if remapper is not null!
    protected void runRemapper(WriteContext ctx) {
        List<Type> paramTypes = new ArrayList<>();
        List<EClass<?>> paramEClasses = new ArrayList<>();
        for (WrappedType parameter : this.params()) {
            paramTypes.add(Type.getType(parameter.raw().raw()));
            paramEClasses.add(parameter.raw());
        }

        MethodVisitor m = ctx.visitor();
        InternalFieldBuilder fields = ctx.fields();

        // Take in parameters, convert them to LuaValue array
        fields.storeAndGet(m, ctx.state(), LuaState.class); // <- 1
        fields.storeAndGet(m, remapper, LuaFunction.class); // <- 2
        AsmUtil.createArray(m, ctx.arrayPos(), paramTypes, LuaValue.class, (mv, i, arg) -> { // internal
            mv.visitVarInsn(arg.getOpcode(ILOAD), i + 1); // <- 2
            AsmUtil.wrapPrimitive(mv, arg); // <- 2 | -> 2 (sometimes)
            fields.storeAndGet(mv, paramEClasses.get(i).wrapPrimitive(), EClass.class); // <- 3
            mv.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false); // <- 2 | -> 2, 3
        }); // <- 3
        ctx.setArrayPos(ctx.arrayPos()+1);
        // Run remapper function
        m.visitMethodInsn(INVOKESTATIC, Owners.VALUE_FACTORY, "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // <- 3 | 3 ->
        m.visitMethodInsn(INVOKESTATIC, Owners.DISPATCH, "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // <- 1 | -> 1, 2, 3
    }

    protected void loadArguments(MethodVisitor m) {
        m.visitVarInsn(ALOAD, 0);
        for (int i = 0; i < this.params().length; i++) {
            m.visitVarInsn(Type.getType(this.params()[i].raw().raw()).getOpcode(ILOAD), i+1);
        }
    }

    protected static void applyTypes(WriteContext ctx, List<Type> remapTypes, List<EClass<?>> remapEClasses) {
        MethodVisitor m = ctx.visitor();
        // Iterate over returned var args & convert to types defined by `currentDefinition`
        final int size = remapTypes.size();
        // Unpacks varargs, putting remapped types onto stack.
        // increases stack size by 1 per loop
        for (int i = 0; i < size; i++) {
            if (i == size-1) m.visitInsn(DUP); // <- 2 (most of the time)
            m.visitLdcInsn(i+1); // <- 3
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.VARARGS, "arg", "(I)Lorg/squiddev/cobalt/LuaValue;", false); // <- 2 | -> 2, 3
            ctx.fields().storeAndGet(m, ctx.state(), LuaState.class); // <- 3
            m.visitInsn(SWAP); // 2 <-> 3
            ctx.fields().storeAndGet(m, remapEClasses.get(i), EClass.class); // <- 4
            m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // -> 2, 3, 4 | <- 2
            m.visitTypeInsn(CHECKCAST, remapTypes.get(i).getClassName()); // <- 2 | -> 2
            if (i == size-1) m.visitInsn(SWAP); // 1 <-> 2
        }
    }
}

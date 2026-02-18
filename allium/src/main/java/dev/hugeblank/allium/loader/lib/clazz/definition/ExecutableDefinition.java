package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.SWAP;

public class ExecutableDefinition {

    protected final String name;
    protected final WrappedType[] params;
    protected final WrappedType returns;
    protected final int access;
    protected final boolean definesFields;

    protected LuaFunction function;

    public ExecutableDefinition(String name, WrappedType[] params, WrappedType returns, int access, boolean definesFields) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.access = access;
        this.definesFields = definesFields;
    }

    @LuaWrapped
    public void put(String key, LuaFunction value) {
        if (function != null) throw new IllegalStateException("Function already registered for constructor.");
        if ((access & Opcodes.ACC_ABSTRACT) != 0) return; // TODO: warn
        if (key.equals(name)) {
            function = value;
        }
    }

    public String name() {
        return name;
    }

    public WrappedType[] params() {
        return params;
    }

    public WrappedType returns() {
        return returns;
    }

    public int access() {
        return access;
    }

    public boolean definesFields() {
        return definesFields;
    }

    public void write(ClassBuilder.BuilderContext bctx) {
        WriteContext ctx = createContext(bctx);

        boolean isStatic = (this.access() & ACC_STATIC) != 0;
        ctx.setArrayPos(Type.getArgumentsAndReturnSizes(ctx.descriptor()) >> 2);
        if (isStatic) ctx.setArrayPos(ctx.arrayPos()-1);
        ctx.setParamOffset(isStatic ? 0 : 1);

        if (definesHandler()) {
            writeStart(ctx);
            if (isStatic) writeFirstElement(ctx);
            writeHook(ctx);
            writeEnd(ctx);
        }
    }

    protected boolean definesHandler() {
        return function != null;
    }

    protected WriteContext createContext(ClassBuilder.BuilderContext bctx) {
        Type[] paramsType = Arrays.stream(this.params()).map(WrappedType::raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        Type returnType = this.returns() == null ? Type.VOID_TYPE : Type.getType(this.returns().raw().raw());
//        boolean isStatic = (this.access() & ACC_STATIC) != 0;
//        boolean isAbstract = (this.access() & ACC_ABSTRACT) != 0;
//        boolean isVoid = this.returns() == null || returnType.getSort() == Type.VOID;

        String desc = Type.getMethodDescriptor(returnType, paramsType);
        return new WriteContext(
            bctx,
            bctx.c().visitMethod(this.access(), this.name(), desc, null, null),
            desc
        );
    }

    protected void writeStart(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();

        m.visitCode();

        m.visitLdcInsn(params.length + ctx.paramOffset());
        m.visitTypeInsn(ANEWARRAY, Owners.LUA_VALUE);
        m.visitVarInsn(ASTORE, ctx.arrayPos());
    }

    protected void writeFirstElement(WriteContext ctx) {
        if ((this.access() & ACC_STATIC) != 0) {
            MethodVisitor m = ctx.visitor();

            String eClass = ctx.fields().storeAndGetComplex(m, EClass::fromJava, EClass.class, ctx.className()); // thisEClass
            m.visitMethodInsn(INVOKESTATIC, Owners.SUPER_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdataFactory;", false); // superUDF
            m.visitVarInsn(ALOAD, 0); // superUDF, this
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.SUPER_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;", false); // superLuaValue
            m.visitTypeInsn(CHECKCAST, Owners.SUPER_USERDATA);
            ctx.fields().get(m, eClass); // superLuaValue, thisEClass
            m.visitMethodInsn(INVOKESTATIC, Owners.PRIVATE_USERDATA_FACTORY, "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdataFactory;", false); // superLuaValue, privateUDF
            m.visitVarInsn(ALOAD, 0); // superLuaValue, privateUDF, this
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA_FACTORY, "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdata;", false); // superLuaValue, privateLuaValue
            m.visitTypeInsn(CHECKCAST, Owners.PRIVATE_USERDATA);
            m.visitInsn(DUP); // superLuaValue, privateLuaValue, privateLuaValue
            m.visitVarInsn(ALOAD, ctx.arrayPos()); // superLuaValue, privateLuaValue, privateLuaValue, array
            m.visitInsn(SWAP); // superLuaValue, privateLuaValue, array, privateLuaValue
            m.visitLdcInsn(0); // superLuaValue, privateLuaValue, array, privateLuaValue, 0
            m.visitInsn(SWAP); // superLuaValue, privateLuaValue, array, 0, privateLuaValue
            m.visitInsn(AASTORE); // superLuaValue, privateLuaValue
            m.visitInsn(SWAP); // privateLuaValue, superLuaValue
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.PRIVATE_USERDATA, "applySuperInstance", "(Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;)V", false); //
        }
    }

    protected void writeHook(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();

        boolean isVoid = Type.getReturnType(ctx.descriptor()).getSort() == Type.VOID;
        int argIndex = ctx.paramOffset();
        var args = Type.getArgumentTypes(ctx.descriptor());

        for (int i = 0; i < args.length; i++) {
            m.visitVarInsn(ALOAD, ctx.arrayPos()); // param
            m.visitLdcInsn(i + ctx.paramOffset()); // param, index
            m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex); // param, index, ???

            if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                AsmUtil.wrapPrimitive(m, args[i]);
            }

            ctx.fields().storeAndGet(m, params[i].real().wrapPrimitive(), EClass.class);
            m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
            m.visitInsn(AASTORE);

            argIndex += args[i].getSize();
        }

        ctx.fields().storeAndGet(m, ctx.state(), LuaState.class); // state
        if (!isVoid) m.visitInsn(DUP); // state, state?
        ctx.fields().storeAndGet(m, function, LuaFunction.class); // state, state?, function
        m.visitVarInsn(ALOAD, ctx.arrayPos()); // state, state, function, luavalue[]
        m.visitMethodInsn(INVOKESTATIC, Owners.VALUE_FACTORY, "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
        m.visitMethodInsn(INVOKESTATIC, Owners.DISPATCH, "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs
    }

    protected void writeEnd(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        Type returnType = Type.getReturnType(ctx.descriptor());

        if (returnType.getSort() != Type.VOID) {

            m.visitMethodInsn(INVOKEVIRTUAL, Owners.VARARGS, "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state, luavalue
            ctx.fields().storeAndGet(m, returns.real().wrapPrimitive(), EClass.class); // state, luavalue, eclass
            m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(returns.real().wrapPrimitive().raw())); // object(return type)

            if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                AsmUtil.unwrapPrimitive(m, returnType); // primitive
            }
        }

        m.visitInsn(returnType.getOpcode(IRETURN));
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

}

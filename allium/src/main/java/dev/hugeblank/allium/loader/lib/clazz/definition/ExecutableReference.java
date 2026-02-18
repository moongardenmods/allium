package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.Owners;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.Arrays;

import static org.objectweb.asm.Opcodes.*;

public class ExecutableReference {

    protected final String name;
    protected final String index;
    protected final WrappedType[] params;
    protected final WrappedType returns;
    protected final int access;
    protected final boolean definesFields;

    protected LuaFunction function;

    public ExecutableReference(String name, String index, WrappedType[] params, WrappedType returns, int access, boolean definesFields) {
        this.name = name;
        this.index = index;
        this.params = params;
        this.returns = returns;
        this.access = access;
        this.definesFields = definesFields;
    }

    public void setFunction(LuaFunction function) {
        this.function = function;
    }

    public String getTypeName() {
        return "executable";
    }

    public boolean isValid() {
        return (this.access & ACC_ABSTRACT) == ACC_ABSTRACT || function != null;
    }

    public String name() {
        return name;
    }

    public String index() {
        return index;
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


        if ((access & ACC_ABSTRACT) == 0) {
            prepareOffsets(ctx);

            ctx.visitor().visitCode();

            writeBeforeHandler(ctx);

            if (definesHandler()) {
                writeArray(ctx);
                writeFirstElement(ctx);
                writeHook(ctx);
            }

            writeReturn(ctx);
        }
        // Method is visited in createContext
        ctx.visitor().visitEnd();
    }

    protected void prepareOffsets(WriteContext ctx) {
        boolean isStatic = (this.access() & ACC_STATIC) != 0;
        ctx.setArrayPos(Type.getArgumentsAndReturnSizes(ctx.descriptor()) >> 2);
        if (isStatic) ctx.setArrayPos(ctx.arrayPos()-1);
        ctx.setParamOffset(isStatic ? 0 : 1);
    }

    protected boolean definesHandler() {
        return function != null;
    }

    protected WriteContext createContext(ClassBuilder.BuilderContext bctx) {
        Type[] paramsType = Arrays.stream(this.params()).map(WrappedType::raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        Type returnType = Type.getType(this.returns().raw().raw());

        String desc = Type.getMethodDescriptor(returnType, paramsType);
        return new WriteContext(
            bctx,
            bctx.c().visitMethod(this.access(), this.name(), desc, null, null),
            desc
        );
    }

    protected void writeBeforeHandler(WriteContext ctx) {}

    protected void writeArray(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        m.visitLdcInsn(params.length + ctx.paramOffset());
        m.visitTypeInsn(ANEWARRAY, Owners.LUA_VALUE);
        m.visitVarInsn(ASTORE, ctx.arrayPos());
    }

    protected void writeFirstElement(WriteContext ctx) {
        if ((this.access() & ACC_STATIC) == 0) {
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
        Type returnType = Type.getReturnType(ctx.descriptor());

        boolean isVoid = returnType.getSort() == Type.VOID;
        int argIndex = ctx.paramOffset();
        var args = Type.getArgumentTypes(ctx.descriptor());

        // Add all parameters to array created by writeArray()
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

        // Execute hook
        ctx.fields().storeAndGet(m, ctx.state(), LuaState.class); // state
        if (!isVoid) m.visitInsn(DUP); // state, state?
        ctx.fields().storeAndGet(m, function, LuaFunction.class); // state, state?, function
        m.visitVarInsn(ALOAD, ctx.arrayPos()); // state, state, function, luavalue[]
        m.visitMethodInsn(INVOKESTATIC, Owners.VALUE_FACTORY, "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
        m.visitMethodInsn(INVOKESTATIC, Owners.DISPATCH, "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs

        // Coerce output in preparation for return.
        if (returnType.getSort() != Type.VOID) {
            m.visitMethodInsn(INVOKEVIRTUAL, Owners.VARARGS, "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state, luavalue
            ctx.fields().storeAndGet(m, returns.real().wrapPrimitive(), EClass.class); // state, luavalue, eclass
            m.visitMethodInsn(INVOKESTATIC, Owners.TYPE_COERCIONS, "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
            m.visitTypeInsn(CHECKCAST, Type.getInternalName(returns.real().wrapPrimitive().raw())); // object(return type)

            if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                AsmUtil.unwrapPrimitive(m, returnType); // primitive
            }
        }
    }

    protected void writeReturn(WriteContext ctx) {
        MethodVisitor m = ctx.visitor();
        Type returnType = Type.getReturnType(ctx.descriptor());

        m.visitInsn(returnType.getOpcode(IRETURN));
        m.visitMaxs(0, 0);
    }

}

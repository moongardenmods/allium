package dev.hugeblank.allium.loader.api;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import dev.hugeblank.allium.util.ClassFieldBuilder;
import dev.hugeblank.allium.util.AsmUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder {
    protected final EClass<?> superClass;
    protected final String className;
    protected final LuaState state;
    protected final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    private final List<EMethod> methods = new ArrayList<>();
    private final ClassFieldBuilder fields;

    @LuaWrapped
    public ClassBuilder(EClass<?> superClass, List<EClass<?>> interfaces, Map<String, Boolean> access, LuaState state) {
        this.state = state;
        this.className = AsmUtil.getUniqueClassName();
        this.fields = new ClassFieldBuilder(className, c);

        this.c.visit(
                V17,
                ACC_PUBLIC | (access.getOrDefault("interface", false) ? ACC_INTERFACE | ACC_ABSTRACT : 0) | (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0),
                className,
                null,
                superClass.name().replace('.', '/'),
                interfaces.stream().map(x -> x.name().replace('.', '/')).toArray(String[]::new)
        );

        if (!access.getOrDefault("interface", false)){
            for (EConstructor<?> superCtor : superClass.constructors()) {
                if (!superCtor.isPublic()) continue;

                var desc = Type.getConstructorDescriptor(superCtor.raw());
                var m = c.visitMethod(superCtor.modifiers(), "<init>", desc, null, null);
                m.visitCode();
                var args = Type.getArgumentTypes(desc);

                m.visitVarInsn(ALOAD, 0);

                int argIndex = 1;

                for (Type arg : args) {
                    m.visitVarInsn(arg.getOpcode(ILOAD), argIndex);

                    argIndex += arg.getSize();
                }

                m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass.raw()), "<init>", desc, false);
                m.visitInsn(RETURN);

                m.visitMaxs(0, 0);
                m.visitEnd();
            }
        }

        this.superClass = superClass;
        this.methods.addAll(this.superClass.methods());
        for (var inrf : interfaces) {
            this.methods.addAll(inrf.methods());
        }
    }

    @LuaWrapped
    public void overrideMethod(@LuaStateArg LuaState state, String methodName, EClass<?>[] parameters, Map<String, Boolean> access, LuaFunction func) throws LuaError {
        var methods = new ArrayList<EMethod>();
        if (access.size() > 1) {
            ScriptRegistry.scriptFromState(state).getLogger().warn("Flags on method override besides 'static' are ignored. For method {}", methodName);
        }
        PropertyResolver.collectMethods(state, this.superClass, this.methods, methodName, access.getOrDefault("static", false), methods::add);

        for (var method : methods) {
            var methParams = method.parameters();

            if (methParams.size() == parameters.length) {
                boolean match = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (!methParams.get(i).parameterType().upperBound().raw().equals(parameters[i].raw())) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    writeMethod(
                        method.name(),
                        methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                        new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
                        method.modifiers() & ~ACC_ABSTRACT,
                        func
                    );

                    return;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find method " + methodName + " in parent class " + superClass.name() + "!");
    }

    @LuaWrapped
    public void createMethod(String methodName, EClass<?>[] params, EClass<?> returnClass, Map<String, Boolean> access, @OptionalArg LuaFunction func) throws LuaError {
        if (func == null && !access.getOrDefault("abstract", false)) throw new LuaError("Expected function, got nil");
        if (func != null && access.getOrDefault("abstract", false)) throw new LuaError("Cannot apply function to abstract method");
        writeMethod(
            methodName,
            Arrays.stream(params).map(x -> new WrappedType(x, x)).toArray(WrappedType[]::new),
            returnClass == null ? null : new WrappedType(returnClass, returnClass),
            ACC_PUBLIC | handleMethodAccess(access),
            func
        );

    }

    private int handleMethodAccess(Map<String, Boolean> access) {
        return (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0) | (access.getOrDefault("static", false) ? ACC_STATIC : 0);
    }

    private void writeMethod(String methodName, WrappedType[] params, WrappedType returnClass, int access, @Nullable LuaFunction func) {
        var paramsType = Arrays.stream(params).map(x -> x.raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        var returnType = returnClass == null ? Type.VOID_TYPE : Type.getType(returnClass.raw.raw());
        var isStatic = (access & ACC_STATIC) != 0;

        var desc = Type.getMethodDescriptor(returnType, paramsType);
        var m = c.visitMethod(access, methodName, desc, null, null);
        int varPrefix = Type.getArgumentsAndReturnSizes(desc) >> 2;
        int thisVarOffset = isStatic ? 0 : 1;

        if (func != null) {
            m.visitCode();

            if (isStatic) varPrefix -= 1;

            m.visitLdcInsn(params.length + thisVarOffset);
            m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
            m.visitVarInsn(ASTORE, varPrefix);

            if (!isStatic) {
                m.visitVarInsn(ALOAD, varPrefix);
                m.visitLdcInsn(0);
                m.visitVarInsn(ALOAD, 0);
                fields.storeAndGetComplex(m, EClass::fromJava, EClass.class, className);
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
                m.visitInsn(AASTORE);
            }

            int argIndex = thisVarOffset;
            var args = Type.getArgumentTypes(desc);
            for (int i = 0; i < args.length; i++) {
                m.visitVarInsn(ALOAD, varPrefix);
                m.visitLdcInsn(i + thisVarOffset);
                m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex);

                if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                    AsmUtil.wrapPrimitive(m, args[i]);
                }

                fields.storeAndGet(m, params[i].real.wrapPrimitive(), EClass.class);
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
                m.visitInsn(AASTORE);

                argIndex += args[i].getSize();
            }

            var isVoid = returnClass == null || returnType.getSort() == Type.VOID;

            fields.storeAndGet(m, state, LuaState.class); // state
            if (!isVoid) m.visitInsn(DUP); // state, state?
            fields.storeAndGet(m, func, LuaFunction.class); // state, state?, function
//            m.visitInsn(SWAP);
            m.visitVarInsn(ALOAD, varPrefix); // state, state, function, luavalue[]
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
//            m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(LuaFunction.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false);
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Dispatch.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs

            if (!isVoid) {
                m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state? luavalue
                fields.storeAndGet(m, returnClass.real.wrapPrimitive(), EClass.class); // state?, luavalue, eclass
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(returnClass.real.wrapPrimitive().raw())); // object(return type)

                if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                    AsmUtil.unwrapPrimitive(m, returnType); // primitive
                }
            }

            m.visitInsn(returnType.getOpcode(IRETURN));

            m.visitMaxs(0, 0);
        }

        m.visitEnd();
    }

    public byte[] getByteArray() {
        return c.toByteArray();
    }

    @LuaWrapped
    public LuaValue build() throws LuaError {
        byte[] classBytes = c.toByteArray();

        Class<?> klass = AsmUtil.defineClass(className, classBytes);

        fields.apply(klass);

        return StaticBinder.bindClass(EClass.fromJava(klass));
    }

    public String getName() {
        return this.className;
    }

    private record WrappedType(EClass<?> raw, EClass<?> real) {
        public static WrappedType fromParameter(EParameter param) {
            return new WrappedType(param.rawParameterType(), param.parameterType().lowerBound());
        }
    }
}

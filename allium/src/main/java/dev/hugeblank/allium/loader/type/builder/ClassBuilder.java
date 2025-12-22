package dev.hugeblank.allium.loader.type.builder;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.MemberFilter;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import dev.hugeblank.allium.loader.type.userdata.PrivateUserdata;
import dev.hugeblank.allium.loader.type.userdata.PrivateUserdataFactory;
import dev.hugeblank.allium.loader.type.userdata.SuperUserdata;
import dev.hugeblank.allium.loader.type.userdata.SuperUserdataFactory;
import dev.hugeblank.allium.util.asm.AsmUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@LuaWrapped
public class ClassBuilder extends AbstractClassBuilder {
    private final LuaState state;
    private final EClass<?> eSuperClass;
    private final List<EMethod> methods = new ArrayList<>();
    private final Map<String, MethodReference> referenceBuffer = new HashMap<>();
    private final FieldBuilder fields;

    @LuaWrapped
    public ClassBuilder(EClass<?> eSuperClass, List<EClass<?>> interfaces, Map<String, Boolean> access, LuaState state) {
        super(
                AsmUtil.getUniqueClassName(),
                eSuperClass.name().replace('.', '/'),
                interfaces.stream()
                        .map(x -> x.name().replace('.', '/'))
                        .toArray(String[]::new),
                ACC_PUBLIC |
                        (access.getOrDefault("interface", false) ? ACC_INTERFACE | ACC_ABSTRACT : 0) |
                        (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0),
                null
        );
        this.state = state;
        this.eSuperClass = eSuperClass;
        this.fields = new FieldBuilder(className, c);

        if (!access.getOrDefault("interface", false)){
            for (EConstructor<?> superCtor : eSuperClass.constructors()) {
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

                m.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(eSuperClass.raw()), "<init>", desc, false);

                m.visitInsn(RETURN);

                m.visitMaxs(0, 0);
                m.visitEnd();
            }
        }

        final List<EMethod> methods = new ArrayList<>(this.eSuperClass.methods());
        EClass<?> eClass = this.eSuperClass;
        while (eClass != null) {
            methods.addAll(eClass.declaredMethods());
            eClass = eClass.superclass();
        }

        for (var inrf : interfaces) {
            methods.addAll(inrf.methods());
        }

        this.methods.addAll(methods.stream().distinct().filter((m) -> m.isPublic() || m.isProtected()).toList());
    }

    @LuaWrapped
    public void put(String key, LuaValue value) {
        if (referenceBuffer.containsKey(key) && value instanceof LuaFunction function) {
            writeMethod(referenceBuffer.remove(key), function);
        } else if (referenceBuffer.containsKey(key) && !(value instanceof LuaFunction)) {
            throw new IllegalStateException("Expected function for '" + key + "' got " + value.typeName());
        } else {
            throw new IllegalStateException("No such method '" + key + "' exists for application on class");
        }
    }

    @LuaWrapped
    public void override(String methodName, EClass<?>[] parameters, Map<String, Boolean> access) throws LuaError {
        var methods = new ArrayList<EMethod>();
        if (access.size() > 1) {
            ScriptRegistry.scriptFromState(state).getLogger().warn("Flags on method override besides 'static' are ignored. For method {}", methodName);
        }
        PropertyResolver.collectMethods(this.methods.stream().filter(new MemberFilter(
                access.getOrDefault("static", false),
                true,
                true,
                false
        )).toList(), methodName, methods::add);

        for (var method : methods) {
            var methParams = method.parameters();

            if (methParams.size() == parameters.length) {
                boolean match = true;
                for (int i = 0; i < parameters.length; i++) {
                    if (!methParams.get(i).parameterType().upperBound().wrapPrimitive().raw().equals(parameters[i].raw())) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    referenceBuffer.put(method.name(), new MethodReference(method.name(),
                            methParams.stream().map(WrappedType::fromParameter).toArray(WrappedType[]::new),
                            new WrappedType(method.rawReturnType(), method.returnType().upperBound()),
                            method.modifiers() & ~ACC_ABSTRACT));
                    return;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find method " + methodName + " in parent class " + eSuperClass.name() + "!");
    }

    @LuaWrapped
    public void method(String methodName, EClass<?>[] parameters, @Nullable EClass<?> returnClass, Map<String, Boolean> access) throws LuaError {
        int accessInt = handleMethodAccess(access);
        if ((accessInt & ACC_ABSTRACT) == 0) {
            referenceBuffer.put(methodName, new MethodReference(methodName,
                    Arrays.stream(parameters).map(x -> new WrappedType(x, x)).toArray(WrappedType[]::new),
                    returnClass == null ? null : new WrappedType(returnClass, returnClass),
                    accessInt));
        }
    }

    private int handleMethodAccess(Map<String, Boolean> access) {
        final int out;
        if (access.getOrDefault("private", false)) {
            out = ACC_PRIVATE;
        } else if (access.getOrDefault("protected", false)) {
            out = ACC_PROTECTED;
        } else {
            out = ACC_PUBLIC;
        }
        return out | (access.getOrDefault("abstract", false) ? ACC_ABSTRACT : 0) | (access.getOrDefault("static", false) ? ACC_STATIC : 0);
    }

    private void writeMethod(MethodReference reference, @Nullable LuaFunction func) {
        var paramsType = Arrays.stream(reference.params()).map(x -> x.raw).map(EClass::raw).map(Type::getType).toArray(Type[]::new);
        var returnType = reference.returns() == null ? Type.VOID_TYPE : Type.getType(reference.returns().raw.raw());
        var isStatic = (reference.access() & ACC_STATIC) != 0;

        var desc = Type.getMethodDescriptor(returnType, paramsType);
        var m = c.visitMethod(reference.access(), reference.name(), desc, null, null);
        int arrayPos = Type.getArgumentsAndReturnSizes(desc) >> 2;
        int thisVarOffset = isStatic ? 0 : 1;

        if (func != null) {
            m.visitCode();

            if (isStatic) arrayPos -= 1;

            m.visitLdcInsn(reference.params().length + thisVarOffset);
            m.visitTypeInsn(ANEWARRAY, Type.getInternalName(LuaValue.class));
            m.visitVarInsn(ASTORE, arrayPos);

            if (!isStatic) {
                String eClass = fields.storeAndGetComplex(m, EClass::fromJava, EClass.class, className); // thisEClass
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(PrivateUserdataFactory.class), "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdataFactory;", false); // privateUDF
                m.visitVarInsn(ALOAD, 0); // privateUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrivateUserdataFactory.class), "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/PrivateUserdata;", false); // privateLuaValue
                m.visitVarInsn(ASTORE, arrayPos+1); //
                fields.get(m, eClass, EClass.class); // thisEClass
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(SuperUserdataFactory.class), "from", "(Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdataFactory;", false); // superUDF
                m.visitVarInsn(ALOAD, 0); // superUDF, this
                m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(SuperUserdataFactory.class), "create", "(Ljava/lang/Object;)Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;", false); // superLuaValue
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(SuperUserdata.class));
                m.visitVarInsn(ALOAD, arrayPos+1); // superLuaValue, privateLuaValue
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(PrivateUserdata.class));
                m.visitInsn(SWAP); // privateLuaValue, superLuaValue
                m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(PrivateUserdata.class), "applySuperInstance", "(Ldev/hugeblank/allium/loader/type/userdata/SuperUserdata;)V", false); //

                m.visitVarInsn(ALOAD, arrayPos); // array
                m.visitLdcInsn(0); // array, 0
                m.visitVarInsn(ALOAD, arrayPos+1); // array, 0, luaValue
                m.visitInsn(AASTORE); //
            }

            int argIndex = thisVarOffset;
            var args = Type.getArgumentTypes(desc);
            for (int i = 0; i < args.length; i++) {
                m.visitVarInsn(ALOAD, arrayPos); // param
                m.visitLdcInsn(i + thisVarOffset); // param, index
                m.visitVarInsn(args[i].getOpcode(ILOAD), argIndex); // param, index, ???

                if (args[i].getSort() != Type.OBJECT || args[i].getSort() != Type.ARRAY) {
                    AsmUtil.wrapPrimitive(m, args[i]);
                }

                fields.storeAndGet(m, reference.params()[i].real.wrapPrimitive(), EClass.class);
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toLuaValue", "(Ljava/lang/Object;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Lorg/squiddev/cobalt/LuaValue;", false);
                m.visitInsn(AASTORE);

                argIndex += args[i].getSize();
            }

            var isVoid = reference.returns() == null || returnType.getSort() == Type.VOID;

            fields.storeAndGet(m, state, LuaState.class); // state
            if (!isVoid) m.visitInsn(DUP); // state, state?
            fields.storeAndGet(m, func, LuaFunction.class); // state, state?, function
            m.visitVarInsn(ALOAD, arrayPos); // state, state, function, luavalue[]
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ValueFactory.class), "varargsOf", "([Lorg/squiddev/cobalt/LuaValue;)Lorg/squiddev/cobalt/Varargs;", false); // state, state?, function, varargs
            m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Dispatch.class), "invoke", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lorg/squiddev/cobalt/Varargs;)Lorg/squiddev/cobalt/Varargs;", false); // state?, varargs

            if (!isVoid) {
                m.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Varargs.class), "first", "()Lorg/squiddev/cobalt/LuaValue;", false); // state? luavalue
                fields.storeAndGet(m, reference.returns().real.wrapPrimitive(), EClass.class); // state?, luavalue, eclass
                m.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TypeCoercions.class), "toJava", "(Lorg/squiddev/cobalt/LuaState;Lorg/squiddev/cobalt/LuaValue;Lme/basiqueevangelist/enhancedreflection/api/EClass;)Ljava/lang/Object;", false); // object
                m.visitTypeInsn(CHECKCAST, Type.getInternalName(reference.returns().real.wrapPrimitive().raw())); // object(return type)

                if (returnType.getSort() != Type.ARRAY && returnType.getSort() != Type.OBJECT) {
                    AsmUtil.unwrapPrimitive(m, returnType); // primitive
                }
            }

            m.visitInsn(returnType.getOpcode(IRETURN));

            m.visitMaxs(0, 0);
        }

        m.visitEnd();
    }

    @LuaWrapped
    public LuaValue build() {
        if (!referenceBuffer.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            Iterator<MethodReference> missingReferences = referenceBuffer.values().iterator();
            while (missingReferences.hasNext()) {
                MethodReference reference = missingReferences.next();
                builder.append(reference.name());
                if (missingReferences.hasNext()) builder.append(", ");
            }
            throw new IllegalStateException("Mising functions for method(s): " + builder);
        }

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

    private record MethodReference(String name, WrappedType[] params, WrappedType returns, int access) {}
}

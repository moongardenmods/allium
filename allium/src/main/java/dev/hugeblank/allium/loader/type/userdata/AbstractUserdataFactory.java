package dev.hugeblank.allium.loader.type.userdata;

import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.*;
import dev.hugeblank.allium.util.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;

public abstract class AbstractUserdataFactory<T, U extends InstanceUserdata<T>> {
    protected final EClass<T> clazz;
    protected final LuaTable metatable;

    private final Candidates candidates;

    protected final @Nullable EMethod indexImpl;
    protected final @Nullable EMethod newIndexImpl;
    protected final @Nullable EMethod lenImpl;
    protected @Nullable LuaTable boundMetatable;

    protected final EClass<? super T> targetClass;
    protected final MemberFilter filter;

    protected final Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();

    AbstractUserdataFactory(EClass<T> clazz, EClass<? super T> targetClass, MemberFilter filter) {
        this.targetClass = targetClass;
        this.filter = filter;
        this.candidates = deriveCandidates(targetClass, filter);
        this.clazz = clazz;
        this.indexImpl = tryFindOp(candidates.methods(), LuaIndex.class, 1, "get");
        this.newIndexImpl = tryFindOp(candidates.methods(), null, 2, "set", "put");
        this.lenImpl = tryFindOp(candidates.methods(), null, 0, "size");


        this.metatable = createMetatable(false);
    }

    AbstractUserdataFactory(EClass<T> clazz, MemberFilter filter) {
        this(clazz, clazz, filter);
    }

    AbstractUserdataFactory(EClass<T> clazz) {
        this(clazz, clazz, MemberFilter.PUBLIC_MEMBERS);
    }



    @Nullable EMethod tryFindOp(List<EMethod> methods, @Nullable Class<? extends Annotation> annotation, int minParams, String... specialNames) {
        EMethod method = null;

        if (annotation != null)

            method = methods
                    .stream()
                    .filter(x ->
                            !x.isStatic()
                                    && x.hasAnnotation(annotation))
                    .findAny()
                    .orElse(null);

        if (method != null) return method;

        method = methods
                .stream()
                .filter(x ->
                        !x.isStatic()
                                && !AnnotationUtils.isHiddenFromLua(x)
                                && ArrayUtils.contains(specialNames, x.name())
                                && x.parameters().size() >= minParams)
                .findAny()
                .orElse(null);

        return method;
    }

    protected Candidates deriveCandidates(EClass<?> clazz, MemberFilter filter) {
        if (filter.equals(MemberFilter.PUBLIC_MEMBERS)) {
            return new Candidates(clazz.methods(), clazz.fields().stream().toList());
        }
        List<EMethod> methods = new ArrayList<>();
        List<EField> fields = new ArrayList<>();
        List<EClass<?>> interfaces = new ArrayList<>();
        Map<String, EClass<?>> nameMap = new HashMap<>();
        while (clazz != null) {
            methods.addAll(clazz.declaredMethods().stream().filter(testMember(nameMap, filter)).toList());
            interfaces.addAll(clazz.interfaces());
            fields.addAll(clazz.declaredFields().stream().filter(testMember(nameMap, filter)).toList());
            clazz = clazz.superclass();
        }
        interfaces.forEach((iface) ->
                methods.addAll(iface.declaredMethods().stream().filter(testMember(nameMap, filter)).toList())
        );
        return new Candidates(methods, fields);
    }

    private static Predicate<EMember> testMember(Map<String, EClass<?>> nameMap, MemberFilter filter) {
        return (m) -> {
            if (!filter.test(m)) return false;
            if (!nameMap.containsKey(m.name())) nameMap.put(m.name(), m.declaringClass());
            return nameMap.get(m.name()).equals(m.declaringClass());
        };
    }

    protected LuaTable createMetatable(boolean isBound) {
        LuaTable metatable = new LuaTable();

        MetatableUtils.applyPairs(metatable, targetClass, cachedProperties, candidates, isBound, filter);

        metatable.rawset("__len", new VarArgFunction() {
            @Override
            protected Varargs invoke(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
                if (lenImpl != null) {
                    try {
                        var instance = TypeCoercions.toJava(state, args.arg(1), targetClass);
                        EClassUse<?> ret = lenImpl.returnTypeUse().upperBound();
                        Object out = lenImpl.invoke(instance);
                        // If out is null, we can assume the length is nil
                        if (out == null) throw new InvalidArgumentException();
                        return TypeCoercions.toLuaValue(out, ret);
                    } catch (IllegalAccessException e) {
                        throw new LuaError(e);
                    } catch (InvocationTargetException e) {
                        var target = e.getTargetException();
                        if (target instanceof LuaError err) {
                            throw err;
                        } else if (target instanceof IndexOutOfBoundsException ignored) {
                        } else {
                            throw new LuaError(target);
                        }
                    } catch (InvalidArgumentException ignore) {}
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get("length");

                if (cachedProperty == EmptyData.INSTANCE) {
                    cachedProperty = PropertyResolver.resolveProperty(targetClass, "length", candidates, filter);
                    cachedProperties.put("length", cachedProperty);
                }

                LuaValue out = cachedProperty.get("length", state, JavaHelpers.checkUserdata(args.arg(1), targetClass.raw()), isBound);
                if (!out.isNil()) return out;

                throw new LuaError("attempt to get length of a " + args.arg(1).toString() + " value");
            }
        });

        metatable.rawset("__index", new VarArgFunction() {

            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
                String name = args.arg(2).checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    if (name.equals("super") && args.arg(1) instanceof PrivateUserdata<?> instance) {
                        cachedProperty = new CustomData<>(instance.superInstance());
                    } else {
                        cachedProperty = PropertyResolver.resolveProperty(targetClass, name, candidates, filter);
                    }

                    cachedProperties.put(name, cachedProperty);
                }
                if (cachedProperty == EmptyData.INSTANCE) {
                    LuaValue output = MetatableUtils.getIndexMetamethod(targetClass, indexImpl, state, args);
                    if (output != null) {
                        return output;
                    }
                }

                return cachedProperty.get(name, state, JavaHelpers.checkUserdata(args.arg(1), targetClass.raw()), isBound);
            }
        });

        metatable.rawset("__newindex", new VarArgFunction() {
            @Override
            public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
                String name = args.arg(2).checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(targetClass, name, candidates, filter);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty == EmptyData.INSTANCE && newIndexImpl != null) {
                    LuaValue output = MetatableUtils.getNewIndexMetamethod(targetClass, newIndexImpl, state, args);
                    if (output != null) {
                        return output;
                    }
                }
                cachedProperty.set(name, state, JavaHelpers.checkUserdata(args.arg(1), targetClass.raw()), args.arg(3));

                return Constants.NIL;
            }
        });

        var comparableInst = clazz.allInterfaces().stream().filter(x -> x.raw() == Comparable.class).findFirst().orElse(null);
        if (comparableInst != null) {
            var bound = comparableInst.typeVariableValues().getFirst().lowerBound();
            metatable.rawset("__lt", new LessFunction(bound));
            metatable.rawset("__le", new LessOrEqualFunction(bound));
        }

        return metatable;
    }

    public abstract U create(Object instance);

    public abstract U createBound(Object instance);

    protected void initBound() {
        if (boundMetatable == null) boundMetatable = createMetatable(true);
    }

    static final class LessFunction extends VarArgFunction {
        private final EClass<?> bound;

        public LessFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @Override
        public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
            Comparable<Object> cmp = JavaHelpers.checkUserdata(args.arg(1), Comparable.class);
            Object cmp2 = JavaHelpers.checkUserdata(args.arg(2), bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0);
        }
    }

    static final class LessOrEqualFunction extends VarArgFunction {
        private final EClass<?> bound;

        public LessOrEqualFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @Override
        public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
            Comparable<Object> cmp = JavaHelpers.checkUserdata(args.arg(1), Comparable.class);
            Object cmp2 = JavaHelpers.checkUserdata(args.arg(2), bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0 || cmp.equals(cmp2));
        }
    }

}

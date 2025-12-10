package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.util.AnnotationUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractUserdataFactory<T, U extends AlliumInstanceUserdata<T>> {
    final EClass<T> clazz;
    final LuaTable metatable;
    final @Nullable EMethod indexImpl;
    final @Nullable EMethod newIndexImpl;

    final Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();

    AbstractUserdataFactory(EClass<T> clazz) {
        this.clazz = clazz;
        List<EMethod> targets = collectMetamethodCandidates();
        this.indexImpl = tryFindOp(targets, LuaIndex.class, 1, "get");
        this.newIndexImpl = tryFindOp(targets, null, 2, "set", "put");
        this.metatable = createMetatable(false);
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

    abstract List<EMethod> collectMetamethodCandidates();

    abstract LuaTable createMetatable(boolean isBound);

    public abstract U create(Object instance);

    public abstract U createBound(Object instance);

    static final class LessFunction extends VarArgFunction {
        private final EClass<?> bound;

        public LessFunction(EClass<?> bound) {
            this.bound = bound;
        }

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
        @Override
        public LuaValue invoke(LuaState state, Varargs args) throws LuaError {
            Comparable<Object> cmp = JavaHelpers.checkUserdata(args.arg(1), Comparable.class);
            Object cmp2 = JavaHelpers.checkUserdata(args.arg(2), bound.raw());

            return ValueFactory.valueOf(cmp.compareTo(cmp2) < 0 || cmp.equals(cmp2));
        }
    }
}

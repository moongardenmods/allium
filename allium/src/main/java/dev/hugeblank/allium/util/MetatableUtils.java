package dev.hugeblank.allium.util;

import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.MemberFilter;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MetatableUtils {
    public static <T> LuaValue getIndexMetamethod(EClass<T> clazz, @Nullable EMethod indexImpl, LuaState state, Varargs args) throws LuaError {
        if (indexImpl != null) {
            LuaValue table = args.arg(1);
            LuaValue key = args.arg(2);
            var parameters = indexImpl.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, key, 1, parameters, List.of());

                if (jargs.length == parameters.size()) {
                    try {
                        var instance = TypeCoercions.toJava(state, table, clazz);
                        EClassUse<?> ret = indexImpl.returnTypeUse().upperBound();
                        Object out = indexImpl.invoke(instance, jargs);
                        // If out is null, we can assume the index is nil
                        if (out == null) throw new InvalidArgumentException();
                        return TypeCoercions.toLuaValue(out, ret);
                    } catch (IllegalAccessException e) {
                        throw new LuaError(e);
                    } catch (InvocationTargetException e) {
                        var target = e.getTargetException();
                        if (target instanceof LuaError err) {
                            throw err;
                        } else if (target instanceof IndexOutOfBoundsException ignored) {
                            // Continue.
                        } else {
                            throw new LuaError(target);
                        }
                    } catch (InvalidArgumentException ignore) {}
                }
            } catch (InvalidArgumentException | IllegalArgumentException e) {
                // Continue.
            }
        }
        return null;
    }

    public static <T> LuaValue getNewIndexMetamethod(EClass<T> clazz, @Nullable EMethod newIndexImpl, LuaState state, Varargs args) throws LuaError {
        if (newIndexImpl != null) {
            var parameters = newIndexImpl.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, args.subargs(2), 1, parameters, List.of());

                if (jargs.length == parameters.size()) {
                    try {
                        var instance = TypeCoercions.toJava(state, args.arg(1), clazz);
                        newIndexImpl.invoke(instance, jargs);
                        return Constants.NIL;
                    } catch (IllegalAccessException e) {
                        throw new LuaError(e);
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof LuaError err)
                            throw err;

                        throw new LuaError(e);
                    }
                }
            } catch (InvalidArgumentException | IllegalArgumentException e) {
                // Continue.
            }
        }
        return null;
    }

    public static <T> void applyPairs(LuaTable metatable, EClass<? super T> clazz, Map<String, PropertyData<? super T>> cachedProperties, Candidates candidates, boolean isBound, MemberFilter filter) {
        metatable.rawset("__pairs", LibFunction.create((state, arg1) -> {
            T instance;
            if (isBound) {
                try {
                    //noinspection unchecked
                    instance = (T) clazz.cast(TypeCoercions.toJava(state, arg1, clazz));
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            } else {
                instance = null;
            }
            final boolean classHasAnnotation = clazz.hasAnnotation(LuaWrapped.class);
            List<EMember> members = candidates.memberStream().filter((member)->
                    !classHasAnnotation || member.hasAnnotation(LuaWrapped.class)
            ).toList();
            List<Varargs> varargs = new ArrayList<>();
            for (EMember member : members) {
                String memberName = member.name();
                if (member.hasAnnotation(LuaWrapped.class)) {
                    String[] names = AnnotationUtils.findNames(member);
                    if (names != null && names.length > 0) {
                        memberName = names[0];
                    }
                }
                PropertyData<? super T> propertyData = cachedProperties.get(memberName);

                if (propertyData == null) { // caching
                    propertyData = PropertyResolver.resolveProperty(clazz, memberName, candidates, filter);
                    cachedProperties.put(memberName, propertyData);
                }

                varargs.add(ValueFactory.varargsOf(LuaString.valueOf(memberName), propertyData.get(
                        memberName,
                        state,
                        instance,
                        isBound
                )));
            }
            Iterator<Varargs> iterator = varargs.listIterator();

            return new VarArgFunction() { // next
                public Varargs invoke(LuaState state, Varargs varargs) {
                    if (!iterator.hasNext()) return Constants.NIL;
                    return iterator.next();
                }
            };
        }));
    }
}

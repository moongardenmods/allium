package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
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
import java.util.stream.Stream;


public class MetatableUtils {
    public static <T> LuaValue getIndexMetamethod(EClass<T> clazz, @Nullable EMethod indexImpl, LuaState state, LuaValue table, LuaValue key) throws LuaError {
        if (indexImpl != null) {
            var parameters = indexImpl.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, key, 1, parameters);

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
                        } else if (target instanceof IndexOutOfBoundsException) {
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

    public static <T> void applyPairs(LuaTable metatable, EClass<T> clazz, Map<String, PropertyData<? super T>> cachedProperties, boolean isBound, boolean forceStatic) {
        metatable.rawset("__pairs", LibFunction.create((state, arg1) -> {
            Stream.Builder<EMember> memberBuilder = Stream.builder();
            clazz.methods().forEach(memberBuilder);
            clazz.fields().forEach(memberBuilder);
            T instance;
            if (isBound) {
                try {
                    instance = clazz.cast(TypeCoercions.toJava(state, arg1, clazz));
                } catch (InvalidArgumentException e) {
                    throw new LuaError(e);
                }
            } else {
                instance = null;
            }
            List<EMember> members = memberBuilder.build().filter((member)->
                    !clazz.hasAnnotation(LuaWrapped.class) ||
                            (
                                    clazz.hasAnnotation(LuaWrapped.class) &&
                                            member.hasAnnotation(LuaWrapped.class)
                            )
            ).toList();
            List<Varargs> varargs = new ArrayList<>();
            for (EMember member : members) {
                if (!forceStatic || member.isStatic()) {
                    String memberName = member.name();
                    if (member.hasAnnotation(LuaWrapped.class)) {
                        String[] names = AnnotationUtils.findNames(member);
                        if (names != null && names.length > 0) {
                            memberName = names[0];
                        }
                    }
                    PropertyData<? super T> propertyData = cachedProperties.get(memberName);

                    if (propertyData == null) { // caching
                        propertyData = PropertyResolver.resolveProperty(state, clazz, memberName, member.isStatic());
                        cachedProperties.put(memberName, propertyData);
                    }

                    if (!Allium.DEVELOPMENT)
                        memberName = ScriptRegistry.scriptFromState(state).getMappings().getMapped(memberName);

                    varargs.add(ValueFactory.varargsOf(LuaString.valueOf(memberName), propertyData.get(
                            memberName,
                            state,
                            instance,
                            isBound
                    )));
                }
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

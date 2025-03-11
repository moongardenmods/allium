package dev.hugeblank.allium.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;


public class MetatableUtils {
    public static <T> LuaValue getIndexMetamethod(EClass<T> clazz, LuaState state, LuaValue table, LuaValue key) throws LuaError {
        EMethod indexImpl = clazz.methods().stream().filter(x -> x.isStatic() && x.hasAnnotation(LuaIndex.class)).findAny().orElse(null);
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

    public static <T> void applyPairs(LuaTable metatable, EClass<T> clazz, Map<String, PropertyData<? super T>> cachedProperties) {
        metatable.rawset("__pairs", LibFunction.create((state) -> {
            Stream.Builder<EMember> memberBuilder = Stream.builder();
            clazz.methods().forEach(memberBuilder);
            clazz.fields().forEach(memberBuilder);
            Stream<Varargs> valueStream = memberBuilder.build().filter((member)->
                    !clazz.hasAnnotation(LuaWrapped.class) ||
                            (
                                    clazz.hasAnnotation(LuaWrapped.class) &&
                                            member.hasAnnotation(LuaWrapped.class)
                            )
            ).map((member)-> {
                String memberName = member.name();
                if (member.hasAnnotation(LuaWrapped.class)) {
                    String[] names = AnnotationUtils.findNames(member);
                    if (names != null && names.length > 0) {
                        memberName = names[0];
                    }
                }
                PropertyData<? super T> propertyData = cachedProperties.get(memberName);

                if (propertyData == null) { // caching
                    propertyData = PropertyResolver.resolveProperty(clazz, memberName, member.isStatic());
                    cachedProperties.put(memberName, propertyData);
                }

                if (!Allium.DEVELOPMENT) memberName = Allium.MAPPINGS.getYarn(memberName);
                try {
                    return ValueFactory.varargsOf(LuaString.valueOf(memberName), propertyData.get(
                            memberName,
                            state,
                            null,
                            false
                    ));
                } catch (LuaError e) {
                    // I have no idea how this could happen, so it'll be interesting if we get an issue
                    // report in the future with it...
                    //noinspection StringConcatenationArgumentToLogCall
                    Allium.LOGGER.warn("Could not get property data for " + memberName, e);
                    return Constants.NIL;
                }
            });

            Iterator<Varargs> iterator = valueStream.iterator();
            return new VarArgFunction() { // next
                public Varargs invoke(LuaState state, Varargs varargs) throws LuaError, UnwindThrowable {
                    if (!iterator.hasNext()) return Constants.NIL;
                    return iterator.next();
                }
            };
        }));
    }
}

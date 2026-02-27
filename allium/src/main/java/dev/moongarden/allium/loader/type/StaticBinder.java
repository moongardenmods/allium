package dev.moongarden.allium.loader.type;

import dev.moongarden.allium.api.LuaIndex;
import dev.moongarden.allium.loader.type.coercion.TypeCoercions;
import dev.moongarden.allium.loader.type.property.*;
import dev.moongarden.allium.loader.type.userdata.ClassUserdata;
import dev.moongarden.allium.util.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StaticBinder {

    private StaticBinder() {}

    public static <T> ClassUserdata<T> bindClass(EClass<T> clazz) {
        return bindClass(clazz, MemberFilter.STATIC_PUBLIC_MEMBERS);
    }

    public static <T> ClassUserdata<T> bindClass(EClass<T> clazz, MemberFilter filter) {
        Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
        LuaTable metatable = new LuaTable();
        Candidates candidates = Candidates.derive(clazz, filter);

        metatable.rawset("__pairs", MetatableUtils.applyPairs(clazz, cachedProperties, candidates, false));

        metatable.rawset("__index", LibFunction.create((state, arg1, arg2) -> {
            if (arg2.isString()) {
                String name = arg2.checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    if (name.equals("class")) {
                        cachedProperty = new CustomData<>(() -> TypeCoercions.toLuaValue(clazz.raw()));
                    } else {
                        cachedProperty = PropertyResolver.resolveProperty(clazz, name, candidates);
                    }

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty != EmptyData.INSTANCE)
                    return cachedProperty.get(name, state, null, false);
            }

            EMethod indexImpl = clazz.methods().stream().filter(x -> x.isStatic() && x.hasAnnotation(LuaIndex.class)).findAny().orElse(null);
            LuaValue output = MetatableUtils.getIndexMetamethod(clazz, indexImpl, state, ValueFactory.varargsOf(arg1, arg2));
            if (output != null) {
                return output;
            }

            if (arg2.type() == Constants.TTABLE) {
                LuaTable table = arg2.checkTable();
                EClass<?>[] typeArgs = new EClass[table.length()];

                for (int i = 0; i < typeArgs.length; i++) {
                    typeArgs[i] = JavaHelpers.asClass(table.rawget(i + 1));
                }

                try {
                    return bindClass(clazz.instantiateWith(List.of(typeArgs)));
                } catch (IllegalArgumentException e) {
                    throw new LuaError(e);
                }
            }

            return Constants.NIL;
        }));

        metatable.rawset("__newindex", LibFunction.create((state, arg1, arg2, arg3) -> {
            String name = arg2.checkString(); // mapped name

            PropertyData<? super T> cachedProperty = cachedProperties.get(name);

            if (cachedProperty == null) {
                cachedProperty = PropertyResolver.resolveProperty(clazz, name, candidates);

                cachedProperties.put(name, cachedProperty);
            }

            cachedProperty.set(name, state, null, arg3);

            return Constants.NIL;
        }));

        // __call passes the class userdata to the invocation function.
        // It is a fortunate coincidence that constructors are technically not static!
        metatable.rawset("__call", new MethodInvocationFunction<>(clazz, clazz.constructors(), "<init>", null, false));

        return new ClassUserdata<>(clazz, metatable);
    }
}
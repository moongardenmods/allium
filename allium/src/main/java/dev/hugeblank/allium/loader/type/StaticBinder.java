package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.api.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.property.*;
import dev.hugeblank.allium.loader.type.userdata.ClassUserdata;
import dev.hugeblank.allium.util.*;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StaticBinder {

    private StaticBinder() {}

    public static <T> ClassUserdata<T> bindClass(EClass<T> clazz) {
        return bindClass(clazz, MemberFilter.PUBLIC_STATIC_MEMBERS);
    }

    public static <T> ClassUserdata<T> bindClass(EClass<T> clazz, MemberFilter filter) {
        Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
        LuaTable metatable = new LuaTable();
        Candidates candidates = new Candidates(clazz.methods(), clazz.fields().stream().toList());

        MetatableUtils.applyPairs(metatable, clazz, cachedProperties, candidates, false, filter);

        metatable.rawset("__index", LibFunction.create((state, arg1, arg2) -> {
            if (arg2.isString()) {
                String name = arg2.checkString();

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    if (name.equals("class")) {
                        cachedProperty = new CustomData<>(TypeCoercions.toLuaValue(clazz.raw()));
                    } else {
                        cachedProperty = PropertyResolver.resolveProperty(clazz, name, candidates, filter);
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
                cachedProperty = PropertyResolver.resolveProperty(clazz, name, candidates, filter);

                cachedProperties.put(name, cachedProperty);
            }

            cachedProperty.set(name, state, null, arg3);

            return Constants.NIL;
        }));

        metatable.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                // TODO: let this function be invoked with java.callWith
                return createInstance(
                        clazz,
                        state,
                        args.subargs(2)
                );
            }
        });

        return new ClassUserdata<>(clazz, metatable);
    }

    private static Varargs createInstance(EClass<?> clazz, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.constructors()) {
            if (AnnotationUtils.isHiddenFromLua(constructor)) continue;

            var parameters = constructor.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, args, 1, parameters, List.of());

                try {
                    EClassUse<?> ret = (EClassUse<?>) constructor.receiverTypeUse();

                    if (ret == null) ret = clazz.asEmptyUse();

                    Object out = constructor.invoke(jargs);
                    return TypeCoercions.toLuaValue(out, ret);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new LuaError(e);
                }
            } catch (InvalidArgumentException e) {
                paramList.add(ArgumentUtils.paramsToPrettyString(parameters));
            }
        }

        StringBuilder error = new StringBuilder("Could not find parameter match for called constructor " +
                clazz.name() +
                "\nThe following are correct argument types:\n"
        );

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }
}
package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.property.PropertyData;
import dev.hugeblank.allium.util.AnnotationUtils;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import dev.hugeblank.allium.util.MetatableUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import dev.hugeblank.allium.loader.type.annotation.LuaIndex;
import dev.hugeblank.allium.loader.type.property.EmptyData;
import dev.hugeblank.allium.loader.type.property.PropertyResolver;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public final class StaticBinder {

    private StaticBinder() {

    }

    public static <T> LuaUserdata bindClass(EClass<T> clazz) {
        Map<String, PropertyData<? super T>> cachedProperties = new HashMap<>();
        LuaFunction getClassFunc = createGetClassFunction(clazz);
        LuaTable metatable = new LuaTable();

        MetatableUtils.applyPairs(metatable, clazz, cachedProperties);

        metatable.rawset("__index", LibFunction.create((state, arg1, arg2) -> {
            if (arg2.isString()) {
                String name = arg2.checkString(); // mapped name

                if (name.equals("getClass")) {
                    return getClassFunc;
                }

                PropertyData<? super T> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, true);

                    cachedProperties.put(name, cachedProperty);
                }

                if (cachedProperty != EmptyData.INSTANCE)
                    return cachedProperty.get(name, state, null, false);
            }

            LuaValue output = MetatableUtils.getIndexMetamethod(clazz, state, arg1, arg2);
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
                cachedProperty = PropertyResolver.resolveProperty(clazz, name, false);

                cachedProperties.put(name, cachedProperty);
            }

            cachedProperty.set(name, state, null, arg3);

            return Constants.NIL;
        }));

        metatable.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return createInstance(
                        clazz,
                        state,
                        args.subargs(2)
                );
            }
        });

        return new LuaUserdata(clazz, metatable);
    }

    private static Varargs createInstance(EClass<?> clazz, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.constructors()) {
            if (AnnotationUtils.isHiddenFromLua(constructor)) continue;

            var parameters = constructor.parameters();
            try {
                var jargs = ArgumentUtils.toJavaArguments(state, args, 1, parameters);

                try { // Get the return type, invoke method, cast returned value, cry.
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

    private static LuaFunction createGetClassFunction(EClass<?> clazz) {
        return LibFunction.create((state) -> TypeCoercions
                .toLuaValue(clazz, EClass.fromJava(EClass.class).instantiateWith(List.of(clazz)))
        );
    }
}
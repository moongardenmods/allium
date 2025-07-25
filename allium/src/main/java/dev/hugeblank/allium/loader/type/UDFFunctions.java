package dev.hugeblank.allium.loader.type;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.util.ArgumentUtils;
import dev.hugeblank.allium.util.JavaHelpers;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class UDFFunctions<T> extends VarArgFunction {
    private final EClass<T> clazz;
    private final List<EMethod> matches;
    private final String name;
    private final T boundReceiver;
    private final boolean isStatic;

    public UDFFunctions(EClass<T> clazz, List<EMethod> matches, String name, T boundReceiver, boolean isStatic) {
        this.clazz = clazz;
        this.matches = matches;
        this.name = name;
        this.boundReceiver = boundReceiver;
        this.isStatic = isStatic;
    }

    @Override
    public Varargs invoke(LuaState state, Varargs args) throws LuaError {
//        if (Allium.DEVELOPMENT) { // Output the script with which the invoking state belongs to (common scripts only)
//            Allium.LOGGER.info("{} {}.{}",
//                    ScriptRegistry.COMMON.hasScript(state) ? ScriptRegistry.COMMON.getScript(state).getId() : "ignore",
//                    clazz.name(),
//                    name
//            );
//        }
        List<String> paramList = new ArrayList<>(); // String for displaying errors more smartly
        StringBuilder error = new StringBuilder("Could not find parameter match for called function \"" +
            name + "\" for \"" + clazz.name() + "\"" +
            "\nThe following are correct argument types:\n"
        );

        try {
//            final T instance;
//            if (boundReceiver != null || isStatic) {
//                instance = boundReceiver;
//            } else if (args.arg(1) instanceof AlliumUserdata<?> userdata) {
//                try {
//                    instance = userdata.toUserdata(clazz);
//                } catch (ClassCastException e) {
//                    throw new LuaError(e);
//                }
//            } else {
//                throw new LuaError("Invocation has no instance"); // This should never happen.
//            }
            T instance = boundReceiver != null || isStatic ? boundReceiver : JavaHelpers.checkUserdata(args.arg(1), clazz.raw());
            for (EMethod method : matches) { // For each matched method from the index call
                var parameters = method.parameters();
                try {
                    var jargs = ArgumentUtils.toJavaArguments(state, args, boundReceiver == null && !isStatic ? 2 : 1, parameters);

                    if (jargs.length == parameters.size()) { // Found a match!
                        try { // Get the return type, invoke method, cast returned value, cry.
                            EClassUse<?> ret = method.returnTypeUse().upperBound();
                            // Some public methods are "inaccessible" despite being public. setAccessible coerces that.
                            method.raw().setAccessible(true);
                            Object out = method.invoke(instance, jargs);
                            if (ret.type().raw() == Varargs.class)
                                return (Varargs) out;
                            else
                                return TypeCoercions.toLuaValue(out, ret);
                        } catch (IllegalAccessException e) {
                            throw new LuaError(e);
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof LuaError err)
                                throw err;

                            throw new RethrowException(e.getTargetException());
                        }
                    }
                } catch (InvalidArgumentException e) {
                    paramList.add(ArgumentUtils.paramsToPrettyString(parameters));
                }
            }
        } catch (Exception e) {
            if (e instanceof LuaError) {
                throw e;
            } else {
                e.printStackTrace();
                error = new StringBuilder(e.toString());
            }
        }

        for (String headers : paramList) {
            error.append("  - ").append(headers).append("\n");
        }
        error.append("got ").append(args.count()-(!isStatic ? 1 : 0)).append(" arguments: \n");
        for (int i = !isStatic ? 2 : 1; i <= args.count(); i++) {
            LuaValue val = args.arg(i);
            if (val instanceof AlliumInstanceUserdata<?> userdata) {
                error.append(userdata.instanceClass().name());
            } else if (val instanceof AlliumClassUserdata<?> userdata) {
                error.append("<class> ").append(userdata.toUserdata().name());
            } else {
                error.append(val.luaTypeName());
            }
            if (i < args.count()) error.append(", ");
        }
        error.append('\n');

        throw new LuaError(error.toString());
    }
}

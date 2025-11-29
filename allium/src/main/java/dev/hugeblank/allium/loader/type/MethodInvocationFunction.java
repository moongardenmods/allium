package dev.hugeblank.allium.loader.type;

import com.mojang.datafixers.util.Pair;
import dev.hugeblank.allium.api.Rethrowable;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.RethrowException;
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;

/// Represents one or more class methods of a given name bundled together in preparation for invocation on the lua side.
public final class MethodInvocationFunction<T> extends VarArgFunction {
    private final EClass<T> clazz;
    private final List<EMethod> matches;
    private final String name;
    private final T boundReceiver;
    private final boolean isStatic;

    public MethodInvocationFunction(EClass<T> clazz, List<EMethod> matches, String name, T boundReceiver, boolean isStatic) {
        this.clazz = clazz;
        this.matches = matches;
        this.name = name;
        this.boundReceiver = boundReceiver;
        this.isStatic = isStatic;
    }

    @Override
    public Varargs invoke(LuaState state, Varargs args) throws LuaError {
        try {
            T instance = boundReceiver != null || isStatic ? boundReceiver : JavaHelpers.checkUserdata(args.arg(1), clazz.raw());
            for (EMethod method : matches) { // For each matched method from the index call
                var parameters = method.parameters();
                try {
                    var javaArgs = ArgumentUtils.toJavaArguments(state, args, boundReceiver == null && !isStatic ? 2 : 1, parameters);

                    if (javaArgs.length == parameters.size()) { // Found a match!
                        try { // Get the return type, invoke method, cast returned value, cry.
                            EClassUse<?> ret = method.returnTypeUse().upperBound();
                            // Some public methods are "inaccessible" despite being public. setAccessible coerces that.
                            method.raw().setAccessible(true);
                            Object out = method.invoke(instance, javaArgs);
                            if (ret.type().raw() == Varargs.class)
                                return (Varargs) out;
                            else
                                return TypeCoercions.toLuaValue(out, ret);
                        } catch (IllegalAccessException e) {
                            throw new LuaError(e);
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof RuntimeException rte && rte instanceof Rethrowable)
                                throw new RethrowException(rte);
                            throw e;
                        }
                    }
                } catch (InvalidArgumentException ignored) {
                }
            }
        } catch (LuaError e) {
            throw e;
        } catch (RethrowException wrapped) {
            wrapped.rethrow();
        } catch (Exception e) {
            final StringBuilder error = new StringBuilder();
            final StringBuilder trace = new StringBuilder();
            e.printStackTrace(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    trace.append((char) b);
                }
            }));
            error.append("Could not call function \"").append(name).append("\" in \"")
                    .append(clazz.name()).append("\"\n")
                    .append("Something could actually be wrong, or one of the arguments passed into this function may be of the wrong type.\n");
            writeGivenAndExpectedTypes(args, error);
            error.append("\nJava Trace:\n").append(trace).append("\nLua Error:");
            throw new LuaError(error.toString());
        }
        final StringBuilder error = new StringBuilder();
        error.append("Could not find parameter match for function \"").append(name).append("\" in \"")
                .append(clazz.name()).append("\"\n");
        writeGivenAndExpectedTypes(args, error);
        throw new LuaError(error.toString());
    }

    private void writeGivenAndExpectedTypes(Varargs args, StringBuilder error) {
        error.append("The following are correct argument types:\n");
        matches.stream().map((match) ->
                        new Pair<>(match.parameters().size(), ArgumentUtils.paramsToPrettyString(match.parameters())))
                .sorted(Comparator.comparingInt(Pair::getFirst)).forEach((match) ->
                        error.append("  - ").append(match.getSecond()).append('\n')
                ); // String for displaying errors more smartly
        error.append("got ").append(args.count()-(!isStatic ? 1 : 0)).append(" arguments: \n  ");
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
    }
}
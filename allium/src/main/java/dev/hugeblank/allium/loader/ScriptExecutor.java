package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.*;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.slf4j.Logger;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptExecutor{
    private final LuaState state;
    private final Script script;
    private final Path path;
    private final Entrypoint entrypoint;

    public ScriptExecutor(Script script, Path path, Entrypoint entrypoint) {
        this.state = new LuaState();
        this.script = script;
        this.path = path;
        this.entrypoint = entrypoint;
    }

    private void createEnvironment() {
        BaseLib.add(state);
        try {
            TableLib.add(state);
            StringLib.add(state);
            CoroutineLib.add(state);
            MathLib.add(state);
            Utf8Lib.add(state);
            Bit32Lib.add(state);

            LibFunction.setGlobalLibrary(state, "script",
                TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class))
            );

            loadLibrary(new PackageLib(script));
            loadLibrary(new MixinLib(script));
            loadLibrary(JavaLib.class);
            loadLibrary(AlliumLib.class);
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
        LuaTable globals = state.globals();
        globals.rawset( "print", new PrintMethod(script.getLogger()) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );
    }

    private void loadLibrary(WrappedScriptLibrary adder) {
        try {
            adder.add(state);
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
    }

    private void loadLibrary(Class<? extends WrappedLibrary> library) {
        try {
            LuaValue lib = StaticBinder.bindClass(EClass.fromJava(library));

            LuaWrapped wrapped = library.getAnnotation(LuaWrapped.class);

            if (wrapped == null || wrapped.name().length == 0)
                throw new IllegalStateException("WrappedLibrary must have a @LuaWrapped annotation with a name!");

            for (String name : wrapped.name()) {
                LibFunction.setGlobalLibrary(state, name, lib);
            }
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize() throws Throwable {
        if (entrypoint.has(Entrypoint.Type.STATIC) && entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            Varargs out = execute(Entrypoint.Type.STATIC);
            execute(Entrypoint.Type.DYNAMIC);
            return out;
        } else if (entrypoint.has(Entrypoint.Type.STATIC)) {
            return execute(Entrypoint.Type.STATIC);
        } else if (entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            execute(Entrypoint.Type.DYNAMIC);
            return Constants.NIL;
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public void preInitialize() throws CompileException, LuaError, IOException {
        createEnvironment();
        if (entrypoint.has(Entrypoint.Type.MIXIN)) execute(Entrypoint.Type.MIXIN);
    }


    public Varargs reload() throws LuaError, CompileException, IOException {
        if (entrypoint.has(Entrypoint.Type.DYNAMIC)) return execute(Entrypoint.Type.DYNAMIC);
        return null;
    }

    private Varargs execute(Entrypoint.Type type) throws IOException, CompileException, LuaError {
        return LuaThread.runMain(state, load(path.resolve(entrypoint.get(type))));
    }

    public LuaFunction load(Path libPath) throws CompileException, LuaError, IOException {
        // data, script.getID() + ":" + entrypoint.get(type))
        InputStream stream = Files.newInputStream(libPath);
        Allium.PROFILER.push(script.getID(), "executor", "load", path.relativize(libPath).toString());
        LuaFunction out = LoadState.load(
                state,
                stream,
                '='+script.getID() + ":/" + path.relativize(libPath),
                state.globals()
        );
        Allium.PROFILER.pop();
        return out;
    }

    private static final class PrintMethod extends VarArgFunction {
        private final Logger logger;

        PrintMethod(Logger logger) {
            this.logger = logger;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) {
            StringBuilder out = new StringBuilder();
            int count = args.count();
            for (int i = 1; i <= count; i++) {
                out.append(args.arg(i).toString());
                if (i != count && count > 1) {
                    out.append('\t');
                }
            }
            logger.info(out.toString());
            return Constants.NIL;
        }
    }
}

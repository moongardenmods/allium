package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.*;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.util.MixinConfigUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
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

public class ScriptExecutor {
    private final LuaState state;
    private final Script script;

    private final MixinLib mixinLib;
    private final PackageLib packageLib;


    public ScriptExecutor(Script script) {
        this.state = new LuaState();
        this.script = script;
        this.mixinLib = new MixinLib(script);
        this.packageLib = new PackageLib(script);
    }

    public LuaState getState() {
        return state;
    }
    private boolean created = false;

    private void createEnvironment(Script script) {
        if (created) return;
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

            loadLibrary(script, state, packageLib);
            loadLibrary(script, state, mixinLib);
            loadLibrary(script, state, JavaLib.class);
            loadLibrary(script, state, AlliumLib.class);
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
        LuaTable globals = state.globals();
        globals.rawset( "print", new PrintMethod(script) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );
        created = true;
    }

    public Varargs execute(String entrypoint) throws IOException, CompileException, LuaError {
        createEnvironment(script);
        return LuaThread.runMain(state, load(script.getPath().resolve(entrypoint.replace('.', '/') + ".lua")));
    }

    public LuaFunction load(Path libPath) throws CompileException, LuaError, IOException {
        // data, script.getID() + ":" + entrypoint.get(type))
        InputStream stream = Files.newInputStream(libPath);
        Allium.PROFILER.push(script.getID(), "executor", "load", script.getPath().relativize(libPath).toString());
        LuaFunction out = LoadState.load(
            state,
            stream,
            '='+script.getID() + ":/" + script.getPath().relativize(libPath),
            state.globals()
        );
        Allium.PROFILER.pop();
        return out;
    }

    public MixinLib getMixinLib() {
        return mixinLib;
    }

    public PackageLib getPackageLib() {
        return packageLib;
    }

    private static void loadLibrary(Script script, LuaState state, WrappedScriptLibrary adder) {
        try {
            adder.add(state);
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
    }

    private static void loadLibrary(Script script, LuaState state, Class<? extends WrappedLibrary> library) {
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

    private static final class PrintMethod extends VarArgFunction {
        private final Script script;

        PrintMethod(Script script) {
            this.script = script;
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
            script.getLogger().info(out.toString());
            return Constants.NIL;
        }
    }

}

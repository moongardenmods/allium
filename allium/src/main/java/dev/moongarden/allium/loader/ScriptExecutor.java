package dev.moongarden.allium.loader;

import dev.moongarden.allium.Allium;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.loader.lib.*;
import dev.moongarden.allium.loader.type.StaticBinder;
import dev.moongarden.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.slf4j.Logger;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.Dispatch;
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
    private final Logger logger;
    private final String id;

    private final MixinLib mixinLib;
    private final PackageLib packageLib;

    public ScriptExecutor(Script script) {
        this.state = new LuaState();
        this.logger = script.getLogger();
        this.id = script.getID();

        this.mixinLib = new MixinLib(script);
        this.packageLib = new PackageLib(script);

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

            loadLibrary(mixinLib);
            loadLibrary(packageLib);
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
            logger.error("Error loading library:", error);
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
            logger.error("Error loading library:", error);
        }
    }

    public LuaState getState() {
        return state;
    }

    public MixinLib getMixinLib() { return mixinLib; }

    public PackageLib getPackageLib() { return packageLib; }

    public LuaValue execute(Path path) throws LuaError, CompileException, IOException {
        InputStream stream = Files.newInputStream(path);
        Allium.PROFILER.push(id, "executor", "load", path.relativize(path).toString());
        LuaFunction func = LoadState.load(
            state,
            stream,
            '=' + id + ":/" + path.getParent().relativize(path),
            state.globals()
        );
        Allium.PROFILER.pop();
        return LuaThread.runMain(state, func).first();
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

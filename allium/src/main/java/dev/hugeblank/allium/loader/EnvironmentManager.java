package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.lib.WrappedLibrary;
import dev.hugeblank.allium.loader.lib.WrappedScriptLibrary;
import dev.hugeblank.allium.loader.lib.AlliumLib;
import dev.hugeblank.allium.loader.lib.JavaLib;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.PackageLib;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

public class EnvironmentManager {
    protected final LuaState state;

    EnvironmentManager() {
        this.state = new LuaState();
    }

    protected void createEnvironment(Script script) {
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

            loadLibrary(script, state, new PackageLib(script));
            loadLibrary(script, state, new MixinLib(script));
            loadLibrary(script, state, JavaLib.class);
            loadLibrary(script, state, AlliumLib.class);
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
        LuaTable globals = state.globals();
        globals.rawset( "print", new PrintMethod(script) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );
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

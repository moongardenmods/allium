package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.LibraryInitializer;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.lib.JavaLib;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.PackageLib;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.LibFunction;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.*;

import java.util.HashSet;
import java.util.Set;

public class EnvironmentManager {
    protected final LuaState state;

    private static final Set<LibraryInitializer> INITIALIZERS = new HashSet<>();
    private static final Set<WrappedLuaLibrary> LIBRARIES = new HashSet<>();

    EnvironmentManager() {
        this.state = new LuaState();
    }

    protected void createEnvironment(Script script) {
        LuaTable globals = state.globals();
        BaseLib.add(globals);
        try {
            TableLib.add(state, globals);
            StringLib.add(state, globals);
            CoroutineLib.add(state, globals);
            MathLib.add(state, globals);
            Utf8Lib.add(state, globals);
            Bit32Lib.add(state, globals);

            LibFunction.setGlobalLibrary(state, globals, "script",
                    TypeCoercions.toLuaValue(script, EClass.fromJava(Script.class))
            );

            loadLibrary(script, state, new PackageLib(script));
            loadLibrary(script, state, new JavaLib());
            loadLibrary(script, state, new MixinLib(script));
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }

        globals.rawset( "print", new PrintMethod(script) );
        globals.rawset( "_HOST", ValueFactory.valueOf(Allium.ID + "_" + Allium.VERSION) );
    }

    protected void applyLibraries(Script script) {
        INITIALIZERS.forEach(initializer -> loadLibrary(script, state, initializer.init(script)));
        LIBRARIES.forEach(library -> loadLibrary(script, state, library));
    }

    private static void loadLibrary(Script script, LuaState state, WrappedLuaLibrary adder) {
        try {
            adder.add(state, state.globals());
        } catch (LuaError error) {
            script.getLogger().error("Error loading library:", error);
        }
    }


    public static void registerLibrary(LibraryInitializer initializer) {
        INITIALIZERS.add(initializer);
    }

    public static void registerLibrary(WrappedLuaLibrary library) {
        LIBRARIES.add(library);
    }

    private static final class PrintMethod extends VarArgFunction {
        private final Script script;

        PrintMethod(Script script) {
            this.script = script;
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) {
            StringBuilder out = new StringBuilder();
            for (int i = 1; i <= args.count(); i++) {
                out.append(args.arg(i).toString());
                if (i != args.count()) {
                    out.append(" ");
                }
            }
            script.getLogger().info(out.toString());
            return Constants.NIL;
        }
    }
}

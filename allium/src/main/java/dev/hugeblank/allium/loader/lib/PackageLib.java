package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Entrypoints;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.CoerceToNative;
import dev.hugeblank.allium.api.LuaIndex;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.api.OptionalArg;
import dev.hugeblank.allium.util.JavaHelpers;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@LuaWrapped(name="package")
public class PackageLib extends WrappedScriptLibrary {
    public final Map<String, LuaValue> preload = new HashMap<>();
    public final Map<String, LuaValue> loaded = new HashMap<>();
    @LuaWrapped public String config = "/\n;\n?\n!\n-";
    @LuaWrapped public String path = "./?.lua;./?/init.lua";

    // When writing a loader in Java, anywhere where a module value can't be determined `nil` should be returned.
    @LuaWrapped public LuaTable searchers = ValueFactory.listOf(
            // Loader to check if module has a loader provided by preload table
            RegisteredFunction.ofS("preload_loader", this::preloadLoader).create(),
            // Loader to check the path internal to the script
            RegisteredFunction.ofS("path_loader", this::pathLoader).create(),
            // Loader to check the path assuming the first value in the path is a script ID
            RegisteredFunction.ofS("external_script_loader", this::externScriptLoader).create(),
            // Loader to check the class files
            RegisteredFunction.of("java_loader", this::javaLoader).create()
    );

    public PackageLib(Script script) {
        super(script);
    }

    @LuaIndex
    public @Nullable @CoerceToNative Map<String, LuaValue> index(String key) {
        if (key.equals("preload")) {
            return preload;
        } else if (key.equals("loaded")) {
            return loaded;
        }
        return null;
    }

    @Override
    public LuaValue add(LuaState state) throws LuaError {
        state.globals().rawset("require", RegisteredFunction.ofS("require", this::require).create());
        return super.add(state);
    }

    private Varargs preloadLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        if (preload.get(args.arg(1).checkString()) instanceof LuaFunction function){
            return Dispatch.call(state, function);
        }
        return Constants.NIL;
    }

    private Varargs pathLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError {
        String modStr = args.arg(1).checkString();
        String pstr = searchPath(modStr, this.path);
        if (pstr != null) {
            Path path = script.getPath().resolve(pstr);
            if (!Files.exists(path)) return Constants.NIL;
            try {
                // Do not allow entrypoints to get loaded from the path.
                boolean loadingEntrypoint = script.isPathForEntrypoint(path, Entrypoints.Type.MAIN);

                if (loadingEntrypoint) {
                    Allium.LOGGER.warn(
                            "Attempted to require an entrypoint of script '{}'." +
                                    " Use require(\"{}\") if you'd like to get the value loaded by the entrypoint script.",
                            script.getID(), script.getID()
                    ); // Slap on the wrist. Allium has already handled loading of the script.
                    return Constants.NIL;
                }
            } catch (IOException ignored) {}
            // Sometimes the loader returns the module *as well* as the path they were loaded from.
            Varargs res = ValueFactory.varargsOf(script.loadLibrary(path), ValueFactory.valueOf(path.toString()));
            return res.first().equals(Constants.NIL) ? Constants.TRUE : res;
        }
        return Constants.NIL;
    }

    private Varargs externScriptLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError {
        Script candidate = ScriptRegistry.getInstance().get(args.arg(1).checkString());
        if (candidate == null) return Constants.NIL;
        if (candidate.getLaunchState().equals(Script.State.UNINITIALIZED)) candidate.initialize();
        return candidate.getModule();
    }

    private LuaValue javaLoader(LuaState state, LuaValue arg) {
        try {
            return StaticBinder.bindClass(JavaHelpers.asClass(arg));
        } catch (LuaError ignored) {}
        return Constants.NIL;
    }

    private String[] config() {
        return config.split("\n");
    }

    @LuaWrapped(name = "searchpath")
    public Varargs searchPathLua(String name, String path, @OptionalArg @Nullable String sep, @OptionalArg @Nullable String rep) {
        StringBuilder estr = new StringBuilder();
        String out = searchPath(name, path, sep, rep, estr);
        if (out == null) {
            return ValueFactory.varargsOf(Constants.NIL, LuaString.valueOf(estr.toString()));
        }
        return LuaString.valueOf(out);
    }

    public @Nullable String searchPath(String name, String path, @Nullable String sep, @Nullable String rep, @Nullable StringBuilder builder) {
        String[] config = config();
        if (sep == null) sep = ".";
        if (rep == null) rep = config[0];

        String[] paths = path.split(String.valueOf(config[1]));
        for (String pathStr : paths) {
            String filledName = pathStr.replace(config[2], name.replace(sep, rep));
            Path p = script.getPath().resolve(filledName);
            if (Files.exists(p) && Files.isReadable(p)) {
                return filledName;
            }
            if (builder != null) builder.append("no file '").append(filledName).append("'\n");
        }

        return null;
    }

    public @Nullable String searchPath(String name, String path) {
        return searchPath(name, path, null, null, null);
    }

    private Varargs require(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        LuaString mod = args.arg(1).checkLuaString();
        String modStr = mod.checkString();
        if (loaded.containsKey(modStr)) return loaded.get(modStr);
        for (int i = 1; i <= searchers.length(); i++) {
            LuaValue loader = searchers.rawget(i);
            if (loader instanceof LuaFunction f) {
                Varargs contents = Dispatch.call(state, f, mod);
                if (contents != Constants.NIL) {
                    LuaValue out = contents.first();
                    loaded.put(modStr, out);
                    return out;
                }
            }
        }
        throw new LuaError("Could not find module " + mod);
    }
}
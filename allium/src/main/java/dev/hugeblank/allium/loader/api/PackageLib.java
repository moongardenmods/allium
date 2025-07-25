package dev.hugeblank.allium.loader.api;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Entrypoint;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.JavaHelpers;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@LuaWrapped(name="package")
public class PackageLib implements WrappedLuaLibrary {
    private final Script script;
    private final Allium.EnvType envType;
    @LuaWrapped @Nullable public final String environment;
    @LuaWrapped public final LuaTable loaders;
    @LuaWrapped public final LuaTable preload;
    @LuaWrapped public final LuaTable loaded;
    @LuaWrapped public String path;

    public PackageLib(Script script, Allium.EnvType envType) {
        this.script = script;
        this.envType = envType;
        this.environment = envType == Allium.EnvType.COMMON ? null : envType.getKey();
        this.path = "./?.lua;./?/init.lua";
        this.preload = new LuaTable();
        this.loaded = new LuaTable();

        // When writing a loader in Java, anywhere where a module value can't be determined `nil` should be returned.
        loaders = ValueFactory.listOf(
                // Loader to check if module has a loader provided by preload table
                RegisteredFunction.ofS("preload_loader", this::preloadLoader).create(),
                // Loader to check the path internal to the script
                RegisteredFunction.ofS("path_loader", this::pathLoader).create(),
                // Loader to check the path assuming the first value in the path is a script ID
                RegisteredFunction.ofS("external_script_loader", this::externScriptLoader).create(),
                // Loader to check the class files
                RegisteredFunction.of("java_loader", this::javaLoader).create()

        );
    }

    @Override
    public LuaValue add(LuaState state, LuaTable globals) throws LuaError {
        globals.rawset("require", RegisteredFunction.ofS("require", this::require).create());
        return WrappedLuaLibrary.super.add(state, globals);
    }

    public Varargs preloadLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        if (preload.rawget(args.arg(1)) instanceof LuaFunction function){
            return Dispatch.call(state, function);
        }
        return Constants.NIL;
    }

    private static boolean isSameFile(Path path, Script script, Entrypoint entrypoint, Entrypoint.Type type) throws IOException {
        return entrypoint.has(type) && Files.isSameFile( path, script.getPath().resolve(entrypoint.get(type)));
    }

    private Varargs pathLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        String modStr = args.arg(1).checkString();
        Entrypoint entrypoint = script.getManifest().entrypoints();
        for (Path path : getPathsFromModule(script, modStr)) {
            try {
                // If the script is requiring its own static entrypoint from the dynamic one, give the value.
                if (isSameFile(path, script, entrypoint, Entrypoint.Type.STATIC))
                    return ValueFactory.varargsOf(script.getModule(), ValueFactory.valueOf(path.toString()));
            } catch (IOException ignored) {}
        }
        return loadFromPaths(state, script, modStr);
    }

    private Varargs externScriptLoader(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        String[] path = args.arg(1).checkString().split("\\.");
        Script candidate = ScriptRegistry.getInstance(envType).get(path[0]);
        if (candidate != null) {
            if (!candidate.isInitialized()) {
                candidate.initialize();
            }
            if (path.length == 1) {
                return candidate.getModule();
            } else {
                return loadFromPaths(state, candidate, toPath(path));
            }
        }
        return Constants.NIL;
    }

    public LuaValue javaLoader(LuaState state, LuaValue arg) {
        try {
            return StaticBinder.bindClass(JavaHelpers.asClass(state, arg));
        } catch (LuaError ignored) {}
        return Constants.NIL;
    }

    private static String toPath(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i < arr.length-1) {
                builder.append("/");
            }
        }
        return builder.toString();
    }

    private Varargs loadFromPaths(LuaState state, Script script, String modStr) throws UnwindThrowable, LuaError {
        List<Path> paths = getPathsFromModule(script, modStr);
        for (Path path : paths) {
            if (!Files.exists(path)) return Constants.NIL;
            try {
                // Do not allow entrypoints to get loaded from the path.
                Entrypoint entrypoint = script.getManifest().entrypoints();
                boolean loadingEntrypoint = isSameFile(path, script, entrypoint, Entrypoint.Type.DYNAMIC) ||
                        isSameFile(path, script, entrypoint, Entrypoint.Type.STATIC);

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
            return ValueFactory.varargsOf(script.loadLibrary(state, path), ValueFactory.valueOf(path.toString()));
        }
        return Constants.NIL;
    }

    private List<Path> getPathsFromModule(Script script, String modStr) {
        List<Path> pathList = new ArrayList<>();
        String[] paths = path.split(";");
        for (String pathStr : paths) {
            Path path = script.getPath().resolve(
                    pathStr.replace("?", modStr.replace(".", "/"))
            );
            pathList.add(path);
        }
        return pathList;
    }

    private Varargs require(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
        LuaString mod = args.arg(1).checkLuaString();
        if (!loaded.rawget(mod).isNil()) return loaded.rawget(mod);
        for (int i = 1; i <= loaders.length(); i++) {
            LuaValue loader = loaders.rawget(i);
            if (loader instanceof LuaFunction f) {
                Varargs contents = Dispatch.call(state, f, mod);
                if (contents != Constants.NIL) {
                    loaded.rawset(mod, contents.arg(1));
                    return contents;
                }
            }
        }
        throw new LuaError("Could not find module " + mod.toString());
    }
}
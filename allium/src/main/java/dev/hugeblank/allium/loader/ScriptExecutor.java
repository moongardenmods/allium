package dev.hugeblank.allium.loader;

import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptExecutor extends EnvironmentManager {
    protected final Script script;
    protected final Path path;
    protected final Entrypoint entrypoint;

    public ScriptExecutor(Script script, Path path, Entrypoint entrypoint) {
        super();
        this.script = script;
        this.path = path;
        this.entrypoint = entrypoint;
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize() throws Throwable {
        applyLibraries(script);
        if (entrypoint.has(Entrypoint.Type.STATIC) && entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            Varargs out = execute(Entrypoint.Type.STATIC);
            execute(Entrypoint.Type.DYNAMIC);
            return out;
        } else if (entrypoint.has(Entrypoint.Type.STATIC)) {
            return execute(Entrypoint.Type.STATIC);
        } else if (entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            return execute(Entrypoint.Type.DYNAMIC);
        } else if (entrypoint.has(Entrypoint.Type.MIXIN)) {
            // It's ok to have a script that's just mixins. I guess.
            return Constants.NIL;
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public void preInitialize() throws CompileException, LuaError, IOException {
        createEnvironment(script);
        if (entrypoint.has(Entrypoint.Type.MIXIN)) execute(Entrypoint.Type.MIXIN);
    }


    public Varargs reload() throws LuaError, CompileException, IOException {
        if (entrypoint.has(Entrypoint.Type.DYNAMIC)) return execute(Entrypoint.Type.DYNAMIC);
        return null;
    }

    private Varargs execute(Entrypoint.Type type) throws IOException, CompileException, LuaError {
        return LuaThread.runMain(state, load(getInputStream(type), script.getID() + ":" + type));
    }

    private InputStream getInputStream(Entrypoint.Type entrypointType) throws IOException {
        return entrypoint.has(entrypointType) ?
                Files.newInputStream(path.resolve(entrypoint.get(entrypointType))) :
                null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException, LuaError {
        return LoadState.load(
                state,
                stream,
                name,
                state.globals()
        );
    }

}

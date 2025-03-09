package dev.hugeblank.allium.loader;

import org.jetbrains.annotations.Nullable;
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

    public ScriptExecutor(Script script, Path path, ScriptRegistry.EnvType envType, Entrypoint entrypoint) {
        super();
        this.script = script;
        this.path = path;
        this.entrypoint = entrypoint;
        createEnvironment(script, envType);
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize() throws Throwable {
        LuaFunction staticFunction;
        LuaFunction dynamicFunction;
        if (entrypoint.has(Entrypoint.Type.STATIC) && entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            staticFunction = this.load(getInputStream(Entrypoint.Type.STATIC), script.getId() + ":static");
            dynamicFunction = this.load(getInputStream(Entrypoint.Type.DYNAMIC), script.getId() + ":dynamic");
            Varargs out = LuaThread.runMain(state, staticFunction);
            LuaThread.runMain(state, dynamicFunction);
            return out;
        } else if (entrypoint.has(Entrypoint.Type.STATIC)) {
            staticFunction = this.load(getInputStream(Entrypoint.Type.STATIC), script.getId());
            return LuaThread.runMain(state, staticFunction);
        } else if (entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            dynamicFunction = this.load(getInputStream(Entrypoint.Type.DYNAMIC), script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }


    public Varargs reload() throws LuaError, CompileException, IOException {
        if (entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            LuaFunction dynamicFunction = this.load(getInputStream(Entrypoint.Type.DYNAMIC), script.getId());
            return LuaThread.runMain(state, dynamicFunction);
        }
        return null;
    }

    private InputStream getInputStream(Entrypoint.Type entrypointType) throws IOException {
        return entrypoint.has(entrypointType) ?
                Files.newInputStream(path.resolve(entrypoint.get(entrypointType))) :
                null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException, LuaError {
        // TODO: Replacing using globals here with an empty table. Does it work?
        return LoadState.load(
                state,
                stream,
                name,
                state.globals()
        );
    }

}

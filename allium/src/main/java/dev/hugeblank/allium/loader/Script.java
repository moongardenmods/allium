package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.MixinConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@LuaWrapped
public class Script implements Identifiable {

    private final Manifest manifest;
    private final Path path;
    private final Logger logger;
    private final ScriptExecutor executor;
    // Whether this script was able to execute (isolated by environment)
    private State initialized = State.UNINITIALIZED;
    // Resources are stored in a weak set so that if a resource is abandoned, it gets destroyed.
    private final Set<ScriptResource> resources = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean destroyingResources = false;

    protected LuaValue module;

    public Script(Reference reference) {
        this.manifest = reference.manifest();
        Allium.PROFILER.push(manifest.id(), "<init>");
        this.path = reference.path();
        this.executor = new ScriptExecutor(this, path, manifest.entrypoints());
        this.logger = LoggerFactory.getLogger('@' + getID());
        Allium.PROFILER.pop();
    }

    public void reload() {
        destroyAllResources();
        try {
            // Reload and set the module if all that's provided is a dynamic script
            this.module = manifest.entrypoints().has(Entrypoint.Type.DYNAMIC) ?
                    executor.reload().arg(1) :
                    this.module;
        } catch (Throwable e) {
            getLogger().error("Could not reload allium script {}", getID(), e);
            unload();
        }

    }

    @LuaWrapped
    public ResourceRegistration registerResource(ScriptResource resource) {
        resources.add(resource);

        return new ResourceRegistration(resource);
    }

    public class ResourceRegistration implements AutoCloseable {
        private final ScriptResource resource;

        private ResourceRegistration(ScriptResource resource) {
            this.resource = resource;
        }

        @Override
        public void close() {
            if (destroyingResources) return;

            resources.remove(resource);
        }
    }

    private void destroyAllResources() {
        if (destroyingResources) throw new IllegalStateException("Tried to recursively destroy resources!");

        destroyingResources = true;

        try {
            for (ScriptResource resource : resources) {
                try {
                    resource.close();
                } catch (Exception e) {
                    getLogger().error("Failed to close script resource", e);
                }
            }
        } finally {
            destroyingResources = false;

            resources.clear();
        }
    }

    public void unload() {
        destroyAllResources();
    }

    public void preInitialize() {
        Allium.PROFILER.push(getID(), "preInitialize");
        if (MixinConfigUtil.isComplete()) {
            getLogger().error("Attempted to pre-initialize after mixin configuration was loaded.");
            return;
        }
        try {
            getExecutor().preInitialize();
        } catch (Throwable e) {
            getLogger().error("Could not pre-initialize allium script {}", getID(), e);
        }
        Allium.PROFILER.pop();
    }

    public void initialize() {
        Allium.PROFILER.push(getID(), "initialize");
        if (initialized == State.UNINITIALIZED) {
            try {
                // Initialize and set module used by require
                this.initialized = State.INITIALIZING; // Guard against duplicate initializations
                this.module = getExecutor().initialize().first();
                this.initialized = State.INITIALIZED; // If all these steps are successful, we can update the state
            } catch (Throwable e) {
                this.module = Constants.NIL;
                getLogger().error("Could not initialize allium script {}", getID(), e);
                unload();
                this.initialized = State.INVALID;
            }
        }
        Allium.PROFILER.pop();
    }

    public State getLaunchState() {
        return initialized;
    }

    // return null if file isn't contained within Scripts path, or if it doesn't exist.
    public LuaValue loadLibrary(LuaState state, Path mod) throws UnwindThrowable, LuaError {
        Allium.PROFILER.push(getID(), "loadLibrary");
        // Ensure the modules parent path is the root path, and that the module exists before loading
        try {
            LuaFunction loadValue = getExecutor().load(mod);
            Allium.PROFILER.push(getID(), "dispatch");
            LuaValue value = Dispatch.call(state, loadValue);
            Allium.PROFILER.pop();
            Allium.PROFILER.pop();
            return value;
        } catch (FileNotFoundException e) {
            Allium.PROFILER.pop();
            // This should never happen, but if it does, boy do I want to know.
            Allium.LOGGER.warn("File claimed to exist but threw a not found exception... </3", e);
            return null;
        } catch (CompileException | IOException e) {
            Allium.PROFILER.pop();
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public String getID() {
        return manifest.id();
    }

    @LuaWrapped
    public String getVersion() {
        return manifest.version();
    }

    @LuaWrapped
    public String getName() {
        return manifest.name();
    }

    @LuaWrapped
    public LuaValue getModule() {
        return module;
    }

    @LuaWrapped
    public LuaState getState() {
        return executor.getState();
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Path getPath() {
        return path;
    }

    public Logger getLogger() {
        return logger;
    }

    public ScriptExecutor getExecutor() {
        return executor;
    }

    @Override
    public String toString() {
        return manifest.name();
    }

    public record Reference(Manifest manifest, Path path) implements Identifiable {

        @Override
        public String getID() {
            return manifest().id();
        }
    }

    public enum State {
        UNINITIALIZED,
        INITIALIZING,
        INITIALIZED,
        INVALID
    }

}

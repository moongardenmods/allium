package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.PackageLib;
import dev.hugeblank.allium.util.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

    private final Set<Path> reloadable = new HashSet<>();
    private boolean shouldDestroyOnReload = false;

    protected LuaValue module;

    public Script(Reference reference) {
        this.manifest = reference.manifest();
        Allium.PROFILER.push(manifest.id(), "<init>");
        this.path = reference.path();
        this.logger = LoggerFactory.getLogger('@' + getID());
        this.executor = new ScriptExecutor(this);
        Allium.PROFILER.pop();
    }

    public void reload() {
        destroyAllResources();
        try {
            shouldDestroyOnReload = true;
            for (Path p : reloadable) {
                executor.execute(p);
            }
            shouldDestroyOnReload = false;
        } catch (Throwable e) {
            getLogger().error("Could not reload allium script {}", getID(), e);
            unload();
        }
    }

    @LuaWrapped
    public void registerReloadable(String location) throws LuaError, CompileException, IOException {
        if (!(initialized == State.INITIALIZING || initialized == State.INITIALIZED)) return;
        PackageLib packageLib = executor.getPackageLib();
        String resolved = packageLib.searchPath(location, packageLib.path);
        if (resolved == null) throw new FileNotFoundException();
        Path p = path.resolve(resolved);
        reloadable.add(p);
        shouldDestroyOnReload = true;
        executor.execute(p);
        shouldDestroyOnReload = false;
    }

    /// Suggest to an event or hook that they should or should not be reloaded.
    /// The result returned by this method should be allowed to be overridden by a script developer.
    public boolean destroyOnReload() {
        return shouldDestroyOnReload;
    }

    @LuaWrapped
    public ResourceRegistration registerResource(ScriptResource resource) {
        resources.add(resource);

        return new ResourceRegistration(resource);
    }

    public void unload() {
        destroyAllResources();
    }

    public void preInitialize() {
        Allium.PROFILER.push(getID(), "preInitialize");
        if (MixinLib.isComplete()) {
            getLogger().error("Attempted to pre-initialize after mixin configuration was loaded.");
            return;
        }
        try {
            if (manifest.entrypoints().has(Entrypoints.Type.MIXIN)) {
                executor.execute(path.resolve(manifest.entrypoints().get(Entrypoints.Type.MIXIN)));
            }
            executor.getMixinLib().applyConfiguration();
        } catch (Throwable e) {
            getLogger().error("Could not pre-initialize allium script {}", getID(), e);
        }
        Allium.PROFILER.pop();
    }

    public void initialize() {
        Allium.PROFILER.push(getID(), "initialize");
        if (initialized == State.UNINITIALIZED) {
            try {
                if (!manifest.entrypoints().has(Entrypoints.Type.MAIN))
                    throw new LuaError("Expected main entrypoint, got none");

                this.initialized = State.INITIALIZING; // Guard against duplicate initializations
                this.module = executor.execute(path.resolve(manifest.entrypoints().get(Entrypoints.Type.MAIN)));
                this.initialized = State.INITIALIZED; // If all these steps are successful, we can update the state
            } catch (Exception e) {
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
    public LuaValue loadLibrary(Path mod) throws LuaError {
        Allium.PROFILER.push(getID(), "loadLibrary");
        // Ensure the modules parent path is the root path, and that the module exists before loading
        try {
            Allium.PROFILER.push(getID(), "dispatch");
            LuaValue value = executor.execute(mod);
            Allium.PROFILER.pop();
            Allium.PROFILER.pop();
            return value;
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

    public Path getPath() {
        return path;
    }

    public Logger getLogger() {
        return logger;
    }

    public MixinLib getMixinLib() { return executor.getMixinLib(); }

    public boolean isPathForEntrypoint(Path path, Entrypoints.Type type) throws IOException {
        Entrypoints entrypoints = manifest.entrypoints();
        return entrypoints.has(type) && Files.isSameFile( path, this.path.resolve(entrypoints.get(type)));
    }

    @Override
    public String toString() {
        return manifest.name();
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

package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.MixinConfigUtil;
import net.fabricmc.api.EnvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

@LuaWrapped()
public class Script implements Identifiable {

    private final Manifest manifest;
    private final Path path;
    private final Logger logger;
    private final ScriptExecutor executor;
    // Whether this script was able to execute (isolated by environment)
    private boolean initialized = false;
    // Resources are stored in a weak set so that if a resource is abandoned, it gets destroyed.
    private final Set<ScriptResource> resources = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean destroyingResources = false;

    protected LuaValue module;

    public Script(Reference reference) {
        this.manifest = reference.manifest();
        this.path = reference.path();
        this.executor = new ScriptExecutor(this, path, manifest.entrypoints());
        this.logger = LoggerFactory.getLogger('@' + getID());
    }

    public void reload() {
        destroyAllResources();
        try {
            // Reload and set the module if all that's provided is a dynamic script
            this.module = manifest.entrypoints().has(Entrypoint.Type.DYNAMIC) ?
                    executor.reload().arg(1) :
                    this.module;
        } catch (Throwable e) {
            //noinspection StringConcatenationArgumentToLogCall
            getLogger().error("Could not reload allium script " + getID(), e);
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
        if (MixinConfigUtil.isComplete()) {
            getLogger().error("Attempted to pre-initialize after mixin configuration was loaded.");
            return;
        }
        try {
            getExecutor().preInitialize();
        } catch (Throwable e) {
            //noinspection StringConcatenationArgumentToLogCall
            getLogger().error("Could not pre-initialize allium script " + getID(), e);
        }
    }

    public void initialize() {
        if (isInitialized()) {
            getLogger().warn("Attempted to initialize while already active!");
            return;
        }
        try {
            // Initialize and set module used by require
            this.module = getExecutor().initialize().arg(1);
            this.initialized = true; // If all these steps are successful, we can set initialized to true
        } catch (Throwable e) {
            //noinspection StringConcatenationArgumentToLogCall
            getLogger().error("Could not initialize allium script " + getID(), e);
            unload();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // return null if file isn't contained within Scripts path, or if it doesn't exist.
    public LuaValue loadLibrary(LuaState state, Path mod) throws UnwindThrowable, LuaError {
        // Ensure the modules parent path is the root path, and that the module exists before loading
        try {
            LuaFunction loadValue = getExecutor().load(Files.newInputStream(mod), mod.getFileName().toString());
            return Dispatch.call(state, loadValue);
        } catch (FileNotFoundException e) {
            // This should never happen, but if it does, boy do I want to know.
            Allium.LOGGER.warn("File claimed to exist but threw a not found exception... </3", e);
            return null;
        } catch (CompileException | IOException e) {
            throw new LuaError(e);
        }
    }

    @LuaWrapped
    public LuaValue getModule() {
        return module;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public Path getPath() {
        return path;
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

    public Mappings getMappings() {
        return Mappings.REGISTRY.get(manifest.mappings());
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

}

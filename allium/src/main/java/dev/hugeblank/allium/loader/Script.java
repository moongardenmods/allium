package dev.hugeblank.allium.loader;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
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
import java.util.*;

@LuaWrapped()
public class Script {

    private final Manifest manifest;
    private final Path path;
    private final ScriptRegistry.EnvType envType;
    private final Logger logger;
    private final ScriptExecutor executor;
    // Whether this script was able to execute (isolated by environment)
    private final Set<ScriptRegistry.EnvType> initialized = new HashSet<>();
    // Resources are stored in a weak set so that if a resource is abandoned, it gets destroyed.
    private final Set<ScriptResource> resources = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean destroyingResources = false;

    protected LuaValue module;

    public Script(Manifest manifest, Path path, ScriptRegistry.EnvType envType) {
        this.manifest = manifest;
        this.path = path;
        this.envType = envType;
        this.executor = new ScriptExecutor(this, path, envType, manifest.entrypoints().get(envType));
        this.logger = LoggerFactory.getLogger('@' + manifest.id());
    }

    // TODO: Move to Bouquet
    public void reload() {
        destroyAllResources();
        try {
            // Reload and set the module if all that's provided is a dynamic script
            this.module = manifest.entrypoints().get(envType).has(Entrypoint.Type.DYNAMIC) ?
                    executor.reload().arg(1) :
                    this.module;
        } catch (Throwable e) {
            //noinspection StringConcatenationArgumentToLogCall
            getLogger().error("Could not reload allium script " + getId(), e);
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

    public void initialize() {
        if (isInitialized()) {
            getLogger().warn("Attempted to initialize while already active!");
            return;
        }
        try {
            // Initialize and set module used by require
            this.module = getExecutor().initialize().arg(1);
            this.initialized.add(envType); // If all these steps are successful, we can set initialized to true
        } catch (Throwable e) {
            getLogger().error("Could not initialize allium script " + getId(), e);
            unload();
        }
    }

    public boolean isInitialized() {
        return initialized.contains(envType);
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
    public String getId() {
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

    //  if ( i % 2 == 0) break;
}

package dev.hugeblank.allium.loader.distribution.fabric;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.compiler.CompileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlliumLanguageAdapter implements LanguageAdapter {

    static Map<Script, Set<Script>> LAUNCH_ORDER = new HashMap<>();

    @Override
    public <T> T create(ModContainer modContainer, String s, Class<T> aClass) throws LanguageAdapterException {
        ModMetadata containerMetadata = modContainer.getMetadata();
        if (!(containerMetadata instanceof LoaderModMetadata)) {
            throw new LanguageAdapterException(
                "Can not load allium entry point container for mod '" + containerMetadata.getId() + "'."
            );
        }
        Script script = ScriptRegistry.getInstance().get(containerMetadata.getId());

        if (aClass.isAssignableFrom(PreLaunchEntrypoint.class)) {
            //noinspection unchecked
            return (T) new AlliumScriptPreLaunchEntrypoint(script, s);
        } else if (aClass.isAssignableFrom(ModInitializer.class)) {
            //noinspection unchecked
            return (T) new AlliumScriptModInitializer(script, s);
        } else {
            try {
                //noinspection unchecked
                return (T) TypeCoercions.toJava(script.getState(), script.getExecutor().execute(s).arg(1), aClass);
            } catch (IOException | CompileException | LuaError | InvalidArgumentException e) {
                throw new LanguageAdapterException(e);
            }
        }
    }

    static {
        ScriptRegistry registry = ScriptRegistry.getInstance();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ModMetadata containerMetadata = mod.getMetadata();
            if (containerMetadata instanceof LoaderModMetadata metadata) {
                boolean needsAllium = false;
                for (EntrypointMetadata entrypoint : metadata.getEntrypoints("main")) {
                    if (entrypoint.getAdapter().equals("allium")) {
                        needsAllium = true;
                        break;
                    }
                }
                if (needsAllium) {
                    Path path = null;
                    for (Path rootPath : mod.getRootPaths()) {
                        Path p = rootPath.resolve("allium");
                        if (Files.exists(p)) {
                            path = p;
                            break;
                        }
                    }
                    if (path == null)
                        throw new RuntimeException("No 'allium' path found for mod '" + containerMetadata.getId() + "'.");

                    Script script = new Script(new FabricManifest(metadata), path);
                    registry.register(script);
                }
            }
        }

        registry.forEach((script) -> {
            ModContainer mod = FabricLoader.getInstance().getModContainer(script.getID()).orElseThrow();
            for (ModDependency dependency : mod.getMetadata().getDependencies()) {
                if (dependency.getKind().isPositive() && registry.has(dependency.getModId()))
                    script.addDependency(registry.get(dependency.getModId()));
            }
        });

    }
}

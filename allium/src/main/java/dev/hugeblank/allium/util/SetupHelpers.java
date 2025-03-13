package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetupHelpers {
    public static void initializeEnvironment(Allium.EnvType containerEnvType) {

        // INITIALIZE EXTENSIONS
        Set<ModContainer> mods = new HashSet<>();
        FabricLoader instance = FabricLoader.getInstance();
        List<EntrypointContainer<AlliumExtension>> containers = switch (containerEnvType) {
            case COMMON -> instance.getEntrypointContainers(Allium.ID, AlliumExtension.class);
            case CLIENT -> instance.getEntrypointContainers(Allium.ID+"-client", AlliumExtension.class);
            case DEDICATED -> instance.getEntrypointContainers(Allium.ID+"-dedicated", AlliumExtension.class);
        };
        containers.forEach((initializer) -> {
            initializer.getEntrypoint().onInitialize();
            mods.add(initializer.getProvider());
        });
        list(mods, containerEnvType, "Initialized " + mods.size() + " extensions:\n",
                (builder, mod) -> builder.append(mod.getMetadata().getId())
        );

        // COLLECT SCRIPTS
        ImmutableSet.Builder<Script> setBuilder = ImmutableSet.builder();
        setBuilder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory(), containerEnvType));
        setBuilder.addAll(FileHelper.getValidModScripts(containerEnvType));
        Set<Script> scripts = setBuilder.build();

        if (scripts.isEmpty()) {
            return;
        }

        for (Script script : scripts) {
            String mappingsID = script.getManifest().mappings();
            if (!Mappings.REGISTRY.has(mappingsID) && Mappings.LOADERS.has(mappingsID)) {
                Mappings.REGISTRY.register(Mappings.of(mappingsID, Mappings.LOADERS.get(mappingsID).load()));
            } else if (!Mappings.LOADERS.has(mappingsID)){
                Allium.LOGGER.error("No mappings exist with ID {} for script {}", mappingsID, script.getID());
                scripts.remove(script);
                continue;
            }
            ScriptRegistry.getInstance(containerEnvType).register(script);
        }
        list(scripts, containerEnvType, "Found " + scripts.size() + " scripts:\n",
                (strBuilder, script) -> strBuilder.append(script.getID())
        );

        // INITIALIZE SCRIPTS
        ScriptRegistry.getInstance(containerEnvType).forEach(Script::initialize);
        Set<Script> set = ScriptRegistry.getInstance(containerEnvType).getAll();
        list(set, containerEnvType, "Initialized " + set.size() + " scripts:\n",
                (builder, script) -> {
                    if (script.isInitialized()) builder.append(script.getID());
                }
        );
    }

    private static <T> void list(Collection<T> collection, Allium.EnvType envType, String initial, BiConsumer<StringBuilder, T> func) {
        if (envType != Allium.EnvType.COMMON) return;
        StringBuilder builder = new StringBuilder(initial);
        collection.forEach((script) -> {
            builder.append("\t- ");
            func.accept(builder, script);
            builder.append("\n");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-1));
    }
}

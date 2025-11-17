package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
        ImmutableSet.Builder<@NotNull Script> setBuilder = ImmutableSet.builder();
        setBuilder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory(), containerEnvType));
        setBuilder.addAll(FileHelper.getValidModScripts(containerEnvType));
        Set<Script> scripts = setBuilder.build();

        if (scripts.isEmpty()) {
            return;
        }

        scripts.forEach((script) -> ScriptRegistry.getInstance(containerEnvType).register(script) );

        list(scripts, containerEnvType, "Found " + scripts.size() + " scripts:\n",
                (strBuilder, script) -> strBuilder.append(script.getID())
        );

        // INITIALIZE SCRIPTS
        ScriptRegistry.getInstance(containerEnvType).forEach(Script::initialize);
        Set<Script> set = ScriptRegistry.getInstance(containerEnvType).getAll().stream()
                .filter(script -> script.getLaunchState().equals(Script.State.INITIALIZED))
                .collect(Collectors.toSet());
        list(set, containerEnvType, "Initialized " + set.size() + " scripts:\n",
                (builder, script) -> builder.append(script.getID())
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

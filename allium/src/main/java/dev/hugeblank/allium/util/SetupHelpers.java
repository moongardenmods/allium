package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.Script;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetupHelpers {

    public static void collectScripts(ScriptRegistry.EnvType containerEnvType) {
        ImmutableSet.Builder<Script> builder = ImmutableSet.builder();
        builder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory(), containerEnvType));
        builder.addAll(FileHelper.getValidModScripts(containerEnvType));

        Set<Script> scripts = builder.build();

        for (Script script : scripts) {
            ScriptRegistry.getInstance(containerEnvType).registerScript(script);
        }

        list(scripts, "Found Scripts: ", (strBuilder, script) -> strBuilder.append(script.getId()));
    }

    public static void initializeScripts(ScriptRegistry.EnvType containerEnvType) {
        ScriptRegistry registry = ScriptRegistry.getInstance(containerEnvType);
        registry.forEach(Script::initialize);
        SetupHelpers.list(registry.getScripts(), "Initialized Scripts: ", (builder, script) -> {
            if (script.isInitialized()) builder.append(script.getId());
        });
    }

    public static void initializeExtensions(@Nullable EnvType envType) {
        final Set<ModContainer> mods = new HashSet<>();
        final FabricLoader instance = FabricLoader.getInstance();
        final List<EntrypointContainer<AlliumExtension>> containers;
        final String affix;
        if (envType == null) {
            containers = instance.getEntrypointContainers(Allium.ID, AlliumExtension.class);
            affix = "Common";
        } else {
            containers = switch (envType) {
                case CLIENT -> instance.getEntrypointContainers(Allium.ID+"-client", AlliumExtension.class);
                case SERVER -> instance.getEntrypointContainers(Allium.ID+"-server", AlliumExtension.class);
            };
            affix = switch (envType) {
                case CLIENT -> "Client";
                case SERVER -> "Server";
            };
        }
        containers.forEach((initializer) -> {
            initializer.getEntrypoint().onInitialize();
            mods.add(initializer.getProvider());
        });
        list(mods, "Initialized " + affix + " Extensions: ",
                (builder, mod) -> builder.append(mod.getMetadata().getId())
        );
    }

    public static <T> void list(Collection<T> collection, String initial, BiConsumer<StringBuilder, T> func) {
        StringBuilder builder = new StringBuilder(initial);
        collection.forEach((script) -> {
            int pre = builder.length();
            func.accept(builder, script);
            if (builder.length() > pre) builder.append(", ");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-2));
    }
}

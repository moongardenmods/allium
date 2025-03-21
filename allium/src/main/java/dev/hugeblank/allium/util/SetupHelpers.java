package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetupHelpers {
    public static void initializeEnvironment() {
        ScriptRegistry registry = ScriptRegistry.getInstance();
        registry.forEach(Script::initialize);
        Set<Script> set = registry.getAll();
        list(set, "Initialized " + set.size() + " scripts:\n",
                (builder, script) -> {
                    if (script.isInitialized()) builder.append(script.getID());
                }
        );
    }

    public static void collectScripts() {
        ImmutableSet.Builder<Script.Reference> setBuilder = ImmutableSet.builder();
        setBuilder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        setBuilder.addAll(FileHelper.getValidModScripts());
        Set<Script.Reference> refs = setBuilder.build();

        if (refs.isEmpty()) {
            return;
        }

        ScriptRegistry registry = ScriptRegistry.getInstance();
        refs.forEach((ref) -> {
            String mappingsID = ref.manifest().mappings();
            if (!Mappings.REGISTRY.has(mappingsID) && Mappings.LOADERS.has(mappingsID)) {
                Mappings.REGISTRY.register(Mappings.of(mappingsID, Mappings.LOADERS.get(mappingsID).load()));
            } else if (!Mappings.LOADERS.has(mappingsID)){
                Allium.LOGGER.error("No mappings exist with ID {} for script {}", mappingsID, ref.manifest().id());
                refs.remove(ref);
                return;
            }
            registry.register(new Script(ref));
        });

        list(refs, "Found " + refs.size() + " scripts:\n",
                (strBuilder, ref) -> strBuilder.append(ref.manifest().id())
        );

        registry.forEach(Script::preInitialize);
    }

    public static void initializeExtensions(EnvType envType) {
        Set<ModContainer> mods = new HashSet<>();
        getExtensionLocations(Allium.ID, mods);
        switch (envType) {
            case CLIENT -> getExtensionLocations(Allium.ID + "-client", mods);
            case SERVER -> getExtensionLocations(Allium.ID + "-server", mods);
        }
        list(mods, "Initialized " + mods.size() + " extensions:\n",
                (builder, mod) -> builder.append(mod.getMetadata().getId())
        );
    }

    private static void getExtensionLocations(String id, Set<ModContainer> mods) {
        FabricLoader.getInstance().getEntrypointContainers(id, AlliumExtension.class)
                .forEach((initializer) -> {
                    initializer.getEntrypoint().onInitialize();
                    mods.add(initializer.getProvider());
                });
    }

    private static <T> void list(Collection<T> collection, String initial, BiConsumer<StringBuilder, T> func) {
        StringBuilder builder = new StringBuilder(initial);
        collection.forEach((script) -> {
            builder.append("\t- ");
            func.accept(builder, script);
            builder.append("\n");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-1));
    }

    public static void initializeDirectories() {
        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        if (Allium.DEVELOPMENT) {
            try {
                if (Files.isDirectory(Allium.DUMP_DIRECTORY))
                    Files.walkFileTree(Allium.DUMP_DIRECTORY, new FileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public @NotNull FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) throws IOException {
                            throw exc;
                        }

                        @Override
                        public @NotNull FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
            } catch (IOException e) {
                throw new RuntimeException("Couldn't delete dump directory", e);
            }
        }
    }
}

package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.lib.MixinLib;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SetupHelpers {
    public static void initializeEnvironment() {
        Allium.PROFILER.push("initializeEnvironment");
        ScriptRegistry registry = ScriptRegistry.getInstance();
        registry.forEach(Script::initialize);
        Set<Script> set = registry.getAll().stream()
                .filter((script) -> script.getLaunchState().equals(Script.State.INITIALIZED))
                .collect(Collectors.toSet());
        list(set, "Initialized " + set.size() + " scripts",
                (builder, script) -> builder.append(script.getID())
        );
        Allium.PROFILER.pop();
    }

    public static void collectScripts() {
        Allium.PROFILER.push("collectScripts");
        ImmutableSet.Builder<Script.@NotNull Reference> setBuilder = ImmutableSet.builder();
        setBuilder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        setBuilder.addAll(FileHelper.getValidModScripts());
        Set<Script.Reference> refs = setBuilder.build();

        if (refs.isEmpty()) {
            Allium.PROFILER.pop();
            return;
        }

        ScriptRegistry registry = ScriptRegistry.getInstance();
        refs.forEach((ref) -> registry.register(new Script(ref)));

        list(refs, "Found " + refs.size() + " scripts",
                (strBuilder, ref) -> strBuilder.append(ref.manifest().id())
        );

        registry.forEach(Script::preInitialize);
        MixinLib.setComplete();
        Allium.PROFILER.pop();
    }

    private static <T> void list(Collection<T> collection, String initial, BiConsumer<StringBuilder, T> func) {
        StringBuilder builder = new StringBuilder(initial);
        builder.append(collection.isEmpty() ? ".\n" : ":\n");
        collection.forEach((script) -> {
            builder.append("\t- ");
            func.accept(builder, script);
            builder.append("\n");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-1));
    }

    public static void initializeDirectories() {
        Allium.PROFILER.push("initializeDirectories");
        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            Allium.PROFILER.pop();
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
                Allium.PROFILER.pop();
                throw new RuntimeException("Couldn't delete dump directory", e);
            }
        }
        Allium.PROFILER.pop();
    }
}

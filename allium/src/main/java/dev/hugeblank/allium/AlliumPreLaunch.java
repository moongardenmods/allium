package dev.hugeblank.allium;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class AlliumPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        clearDumpDirectory();
    }

    private static void clearDumpDirectory() {
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

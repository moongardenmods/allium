package dev.hugeblank.allium;

import dev.architectury.platform.Platform;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.Mappings;
import dev.hugeblank.allium.util.SetupHelpers;
import dev.hugeblank.allium.util.YarnLoader;
import net.fabricmc.api.EnvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class Allium {
    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final boolean DEVELOPMENT = Platform.isDevelopmentEnvironment();
    public static final Path DUMP_DIRECTORY = Platform.getGameFolder().resolve("allium-dump");
    public static final String VERSION = Platform.getMod(ID).getVersion();
    public static Mappings MAPPINGS;

    public static void init() {
        clearDumpDirectory();
        
        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        LOGGER.info("Loading NathanFudge's Yarn Remapper");
        MAPPINGS = YarnLoader.init();

        SetupHelpers.initializeExtensions(null);
        SetupHelpers.collectScripts(ScriptRegistry.EnvType.COMMON);
        SetupHelpers.initializeScripts(ScriptRegistry.EnvType.COMMON);
    }

    public static void initClient() {
        SetupHelpers.initializeExtensions(EnvType.CLIENT);
        SetupHelpers.collectScripts(ScriptRegistry.EnvType.CLIENT);
        SetupHelpers.initializeScripts(ScriptRegistry.EnvType.CLIENT);
    }

    public static void initServer() {
        SetupHelpers.initializeExtensions(EnvType.SERVER);
        SetupHelpers.collectScripts(ScriptRegistry.EnvType.SERVER);
        SetupHelpers.initializeScripts(ScriptRegistry.EnvType.SERVER);
    }

    private static void clearDumpDirectory() {
        if (Allium.DEVELOPMENT) {
            try {
                if (Files.isDirectory(Allium.DUMP_DIRECTORY))
                    Files.walkFileTree(Allium.DUMP_DIRECTORY, new FileVisitor<>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            throw exc;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
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

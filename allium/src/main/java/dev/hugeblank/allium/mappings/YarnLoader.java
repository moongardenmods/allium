// Derived from:
// https://github.com/natanfudge/Not-Enough-Crashes/blob/b5119154d9b7ba8f08656b82e87158860f0e5489/fabric/src/main/java/fudge/notenoughcrashes/fabric/YarnVersion.java
//  and https://github.com/natanfudge/Not-Enough-Crashes/blob/b5119154d9b7ba8f08656b82e87158860f0e5489/fabric/src/main/java/fudge/notenoughcrashes/fabric/StacktraceDeobfuscator.java
// Nathanfudge has the entirety of my heart and I owe them my life for this
package dev.hugeblank.allium.mappings;

import com.google.common.net.UrlEscapers;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;

public class YarnLoader implements MappingsLoader {
    private static final Path CACHED_MAPPINGS = FileHelper.MAPPINGS_CFG_DIR.resolve("yarn-mappings-" +  SetupHelpers.getGameVersion() + ".tiny");
    private static final Path VERSION_FILE = FileHelper.MAPPINGS_CFG_DIR.resolve("yarn-version.txt");

    @Override
    public void load(MappingVisitor visitor) {
        try {
            // It's imperative that allium has these mappings otherwise all methods
            // in production will be intermediary names. not good.
            if (!Files.exists(CACHED_MAPPINGS)) {
                String yarnVersion = YarnVersion.getLatestBuildForCurrentVersion();

                Allium.LOGGER.info("Downloading yarn mappings: {} for first launch", yarnVersion);

                String encodedYarnVersion = UrlEscapers.urlFragmentEscaper().escape(yarnVersion);
                // Download V2 jar
                String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/yarn/" + encodedYarnVersion + "/yarn-" + encodedYarnVersion + "-v2.jar";

                File jarFile = FileHelper.MAPPINGS_CFG_DIR.resolve("yarn-mappings.jar").toFile();
                jarFile.deleteOnExit();
                try {
                    FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
                } catch (IOException e) {
                    Allium.LOGGER.error("Failed to download mappings!");
                    throw e;
                }

                try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
                    Files.copy(jar.getPath("mappings/mappings.tiny"), CACHED_MAPPINGS, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Allium.LOGGER.error("Failed to extract mappings!");
                    throw e;
                }
            }
            try {
                Tiny2FileReader.read(Files.newBufferedReader(CACHED_MAPPINGS), visitor);
            } catch (IOException e) {
                Allium.LOGGER.error("Could not load mappings");
                throw e;
            }
        } catch (Exception e) {
            Allium.LOGGER.error("Failed to load mappings!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getID() {
        return "yarn";
    }

    private static class YarnVersion {
        private static final String YARN_API_ENTRYPOINT = "https://meta.fabricmc.net/v2/versions/yarn/" + SetupHelpers.getGameVersion();
        private static String versionMemCache = null;
        public int build;
        public String version;


        public static String getLatestBuildForCurrentVersion() throws IOException {
            if (versionMemCache == null) {
                if (!Files.exists(VERSION_FILE)) {
                    YarnVersion[] versions = SetupHelpers.fetch(YARN_API_ENTRYPOINT, YarnVersion[].class);
                    if (versions.length == 0) {
                        throw new IllegalStateException("No yarn versions were received at the API endpoint.");
                    }
                    String version = Arrays.stream(versions).max(Comparator.comparingInt(v -> v.build)).get().version;
                    Files.write(VERSION_FILE, version.getBytes());
                    versionMemCache = version;
                } else {
                    versionMemCache = new String(Files.readAllBytes(VERSION_FILE));
                }
            }

            return versionMemCache;
        }

    }
}

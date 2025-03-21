package dev.hugeblank.allium.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

public class FileHelper {
    /* Allium Script directory spec
      /allium
        /<unique dir name> | unique directory name, bonus point if using the namespace ID
          /<libs and stuff>
          manifest.json |  File containing key information about the script. ID, Name, Version, Entrypoint locations
    */

    public static final Path SCRIPT_DIR = FabricLoader.getInstance().getGameDir().resolve(Allium.ID);
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Allium.ID);
    public static final Path PERSISTENCE_DIR = FabricLoader.getInstance().getConfigDir().resolve(Allium.ID + "_persistence");
    public static final Path MAPPINGS_CFG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Allium.ID + "_mappings");
    public static final String MANIFEST_FILE_NAME = "manifest.json";

    public static Path getScriptsDirectory() {
        if (!Files.exists(SCRIPT_DIR)) {
            Allium.LOGGER.warn("Missing allium directory, creating one for you");
            try {
                Files.createDirectory(SCRIPT_DIR);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create allium directory", new FileSystemException(SCRIPT_DIR.toAbsolutePath().toString()));
            }
        }
        return SCRIPT_DIR;
    }

    public static Set<Script.Reference> getValidDirScripts(Path p) {
        Set<Script.Reference> out = new HashSet<>();
        try {
            Stream<Path> files = Files.list(p);
            files.forEach((scriptDir) -> {
                if (Files.isDirectory(scriptDir)) {
                    addReference(out, scriptDir);
                } else {
                    try {
                        FileSystem fs = FileSystems.newFileSystem(scriptDir); // zip, tarball, whatever has a provider.
                        addReference(out, fs.getPath("/"));
                    } catch (IOException | ProviderNotFoundException ignored) {}
                }
            });
            files.close();
        } catch (IOException e) {
            Allium.LOGGER.error("Could not read from scripts directory", e);
        }
        return out;
    }

    private static void addReference(Set<Script.Reference> scripts, Path path) {
        try {
            BufferedReader reader = Files.newBufferedReader(path.resolve(MANIFEST_FILE_NAME));
            Manifest manifest = new Gson().fromJson(reader, Manifest.class);
            if (manifest.isComplete()) {
                scripts.add(new Script.Reference(manifest, path));
            } else {
                Allium.LOGGER.error("Incomplete manifest on path {}", path);
            }
        } catch (IOException e) {
            //noinspection StringConcatenationArgumentToLogCall
            Allium.LOGGER.error("Could not find " + MANIFEST_FILE_NAME  + " file on path " + path, e);
        }
    }

    // TODO: Test in prod
    public static Set<Script.Reference> getValidModScripts() {
        Set<Script.Reference> out = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach((container) -> {
            ModMetadata metadata = container.getMetadata();
            if (metadata.containsCustomValue(Allium.ID)) {
                switch (metadata.getCustomValue(Allium.ID).getType()) {
                    case OBJECT -> {
                        CustomValue.CvObject value = metadata.getCustomValue(Allium.ID).getAsObject();
                        Manifest man = makeManifest( // Make a manifest using the default values, use optional args otherwise.
                                value,
                                metadata.getId(),
                                metadata.getVersion().getFriendlyString(),
                                metadata.getName()
                        );
                        if (!man.isComplete()) { // Make sure the manifest exists and has an entrypoint
                            Allium.LOGGER.error("Could not read manifest from script with ID {}", metadata.getId());
                            return;
                        }
                        if (man.entrypoints() != null) {
                            if (!man.entrypoints().valid()) {
                                Allium.LOGGER.error("Invalid entrypoints from script with ID {}", metadata.getId());
                                return;
                            }
                            Script.Reference ref = referenceFromContainer(man, container);
                            if (ref == null) {
                                Allium.LOGGER.error("Could not find entrypoints for script with ID {}", metadata.getId());
                                return;
                            }
                            out.add(ref);
                        }
                    }
                    case ARRAY -> {
                        CustomValue.CvArray value = metadata.getCustomValue(Allium.ID).getAsArray();
                        int i = 0; // Index for developer to debug their mistakes
                        for (CustomValue v : value) { // For each array value
                            if (v.getType() == CustomValue.CvType.OBJECT) {
                                CustomValue.CvObject obj = v.getAsObject();
                                Manifest man = makeManifest(obj); // No optional arguments here.
                                if (!man.isComplete()) {
                                    Script.Reference ref = referenceFromContainer(man, container);
                                    if (ref != null) {
                                        out.add(ref);
                                    }
                                } else { // a value was missing. Be forgiving, and continue parsing
                                    Allium.LOGGER.warn("Malformed manifest at index {} of allium array block in fabric.mod.json of mod '{}'", i, metadata.getId());
                                }
                                i++;
                            } else {
                                Allium.LOGGER.warn("Expected object at index {} of allium array block in fabric.mod.json of mod '{}'", i, metadata.getId());
                            }
                        }
                    }
                    default -> Allium.LOGGER.error("allium block for mod '{}' not of type JSON Object or Array", metadata.getId());
                }
            }
        });
        return out;
    }

    private static Script.Reference referenceFromContainer(Manifest man, ModContainer container) {
        AtomicReference<Script.Reference> out = new AtomicReference<>();
        container.getRootPaths().forEach((path) -> {
            Entrypoint entrypoints = man.entrypoints();
            if (exists(entrypoints, path, Entrypoint.Type.STATIC) || exists(entrypoints, path, Entrypoint.Type.DYNAMIC) || exists(entrypoints, path, Entrypoint.Type.MIXIN)) {
                // This has an incidental safeguard in the event that if multiple root paths have the same script
                // the most recent script loaded will just *overwrite* previous ones.
                out.set(new Script.Reference(man, path));
            }
        });
        return out.get();
    }

    private static boolean exists(Entrypoint entrypoints, Path path, Entrypoint.Type type) {
        return entrypoints.has(type) && path.resolve(entrypoints.get(type)).toFile().exists();
    }

    // TODO: Move this method and the one below to bouquet.
    public static JsonElement getConfig(Script script) throws IOException {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getID() + ".json");
        if (Files.exists(path)) {
            return JsonParser.parseReader(Files.newBufferedReader(path));
        }
        return null;
    }

    public static void saveConfig(Script script, JsonElement element) throws IOException {
        Path path = FileHelper.CONFIG_DIR.resolve(script.getID() + ".json");
        Files.deleteIfExists(path);
        OutputStream outputStream = Files.newOutputStream(path);
        String jstr = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element);
        Allium.LOGGER.info(jstr);
        outputStream.write(jstr.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
    }

    private static Manifest makeManifest(CustomValue.CvObject value) {
        return makeManifest(value, null, null, null);
    }

    private static Manifest makeManifest(
            CustomValue.CvObject value,
            @Nullable String optId,
            @Nullable String optVersion,
            @Nullable String optName
    ) {
        return new Manifest(
                getOrDefault(value, "id", optId, CustomValue::getAsString),
                getOrDefault(value, "version", optVersion, CustomValue::getAsString),
                getOrDefault(value, "name", optName, CustomValue::getAsString),
                getOrDefault(value, "mappings", null, CustomValue::getAsString),
                makeEntrypointContainer(getOrDefault(value, "entrypoints", null, CustomValue::getAsObject))
        );
    }

    private static <T> T getOrDefault(CustomValue.CvObject source, String key, T def, Function<CustomValue, T> getAs) {
        return source.get(key).getType() == CustomValue.CvType.STRING ? getAs.apply(source.get(key)) : def;
    }

    private static Entrypoint makeEntrypointContainer(CustomValue.CvObject entrypointsObject) {
        Map<Entrypoint.Type, String> entrypointMap = new HashMap<>();
        for (Entrypoint.Type type : Entrypoint.Type.values()) {
            if (entrypointsObject.containsKey(type.getKey())) {
                entrypointMap.put(type, entrypointsObject.get(type.getKey()).getAsString());
            }
        }
        return new Entrypoint(entrypointMap);
    }
}

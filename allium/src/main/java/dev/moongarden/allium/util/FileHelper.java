package dev.moongarden.allium.util;

import com.google.gson.Gson;
import dev.moongarden.allium.Allium;
import dev.moongarden.allium.loader.Entrypoints;
import dev.moongarden.allium.loader.Manifest;
import dev.moongarden.allium.loader.Script;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
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
                        Stream<Path> stream = Files.list(fs.getPath("/"));
                        List<Path> list = stream.toList();
                        stream.close();
                        if (list.size() == 1) {
                            Path internalPath = list.getFirst();
                            if (Files.isDirectory(internalPath)) {
                                addReference(out, internalPath);
                            }
                        } else {
                            addReference(out, fs.getPath("/"));
                        }

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
            // TODO does this work?
            Allium.LOGGER.error("Could not find {} file on path {}", MANIFEST_FILE_NAME, path, e);
        }
    }

    public static Set<Script.Reference> getValidModScripts() {
        Set<Script.Reference> out = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach((container) -> {
            ModMetadata metadata = container.getMetadata();
            if (metadata.containsCustomValue(Allium.ID)) {
                switch (metadata.getCustomValue(Allium.ID).getType()) {
                    case OBJECT -> {
                        CustomValue.CvObject alliumObject = metadata.getCustomValue(Allium.ID).getAsObject();
                        if (alliumObject.containsKey("scripts") && alliumObject.get("scripts").getType().equals(CustomValue.CvType.ARRAY)) {
                            alliumObject.get("scripts").getAsArray().forEach((scriptObject) -> {
                                if (scriptObject.getType().equals(CustomValue.CvType.OBJECT)) {
                                Manifest man = makeManifest( // Make a manifest using the default values, use optional args otherwise.
                                        scriptObject.getAsObject(),
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
                            });
                        }
                        if (alliumObject.containsKey("paths") && alliumObject.get("paths").getType().equals(CustomValue.CvType.ARRAY)) {
                            alliumObject.get("paths").getAsArray().forEach((pathsArray) -> {
                                if (pathsArray.getType().equals(CustomValue.CvType.STRING)) {
                                    String suffix = pathsArray.getAsString();
                                    container.getRootPaths().forEach((path) -> {
                                        if (Files.exists(path.resolve(suffix))) {
                                            Set<Script.Reference> scripts = getValidDirScripts(path.resolve(suffix));
                                            out.addAll(scripts);
                                        }
                                    });
                                }
                            });
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
            Entrypoints entrypoints = man.entrypoints();
            if (exists(entrypoints, path, Entrypoints.Type.MAIN) || exists(entrypoints, path, Entrypoints.Type.MIXIN)) {
                // This has an incidental safeguard in the event that if multiple root paths have the same script
                // the most recent script loaded will just *overwrite* previous ones.
                out.set(new Script.Reference(man, path));
            }
        });
        return out.get();
    }

    private static boolean exists(Entrypoints entrypoints, Path path, Entrypoints.Type type) {
        return entrypoints.has(type) && Files.exists(path.resolve(entrypoints.get(type)));
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
                makeEntrypointContainer(getOrDefault(value, "entrypoints", null, CustomValue::getAsObject))
        );
    }

    private static <T> T getOrDefault(CustomValue.CvObject source, String key, T def, Function<CustomValue, T> getAs) {
        return source.containsKey(key) ? getAs.apply(source.get(key)) : def;
    }

    private static Entrypoints makeEntrypointContainer(CustomValue.CvObject entrypointsObject) {
        Map<Entrypoints.Type, String> entrypointMap = new HashMap<>();
        for (Entrypoints.Type type : Entrypoints.Type.values()) {
            if (entrypointsObject.containsKey(type.getKey())) {
                entrypointMap.put(type, entrypointsObject.get(type.getKey()).getAsString());
            }
        }
        return new Entrypoints(entrypointMap);
    }
}

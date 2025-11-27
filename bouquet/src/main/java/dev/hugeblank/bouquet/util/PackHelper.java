package dev.hugeblank.bouquet.util;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PackHelper {
    private final List<Script> scriptsWithPacks = new ArrayList<>();
    public PackHelper() {

        ScriptRegistry.getInstance().forEach(
                (script) -> {
                    Path resources = script.getPath().resolve("resources");
                    if (Files.exists(resources)) scriptsWithPacks.add(script);
                }
        );
    }

    public List<Script> getScriptsWithPacks() {
        return List.copyOf(scriptsWithPacks);
    }

    public Pack.ResourcesSupplier createPack() {
        Map<String, PackResources> scriptPacks = new HashMap<>();
        scriptsWithPacks.forEach(
                (script) -> {
                    Path resources = script.getPath().resolve("resources");
                    if (Files.isDirectory(resources)) {
                        scriptPacks.put(script.getID(), new PathPackResources(
                                new PackLocationInfo(
                                        script.getID(), Component.literal(script.getName()),
                                        PackSource.BUILT_IN, Optional.empty()
                                ),
                                resources
                        ));
                    } else {
                        scriptPacks.put(script.getID(), new FilePackResources.FileResourcesSupplier(script.getPath())
                                .openPrimary(new PackLocationInfo(
                                        script.getID(), Component.literal(script.getName()),
                                        PackSource.BUILT_IN, Optional.empty()
                                ))
                        );
                    }
                }
        );
        return new BouquetResourcesSupplier(scriptPacks);
    }


    @SuppressWarnings("ClassCanBeRecord")
    public static class BouquetResourcesSupplier implements Pack.ResourcesSupplier {
        private final Map<String, PackResources> scriptPacks;

        public BouquetResourcesSupplier(final Map<String, PackResources> scriptPacks) {
            this.scriptPacks = scriptPacks;
        }

        public @NotNull PackResources openPrimary(final @NotNull PackLocationInfo location) {
            return new BouquetPackResources(location);
        }

        public @NotNull PackResources openFull(final @NotNull PackLocationInfo location, final Pack.Metadata metadata) {
            PackResources primary = this.openPrimary(location);
            List<String> overlays = metadata.overlays();
            if (overlays.isEmpty()) {
                return primary;
            } else {
                List<PackResources> overlayResources = overlays.stream().map(scriptPacks::get).toList();
                return new CompositePackResources(primary, overlayResources);
            }
        }
    }
}

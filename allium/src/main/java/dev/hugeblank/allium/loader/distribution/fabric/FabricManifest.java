package dev.hugeblank.allium.loader.distribution.fabric;

import dev.hugeblank.allium.loader.Manifest;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;

import java.util.List;

public class FabricManifest extends Manifest {
    private final boolean hasMain;
    private final LoaderModMetadata metadata;

    public FabricManifest(LoaderModMetadata metadata) {
        super(metadata.getId(), metadata.getVersion().getFriendlyString(), metadata.getName());

        boolean hasMain = false;
        List<EntrypointMetadata> entrypoints = metadata.getEntrypoints("main");
        for (EntrypointMetadata entrypoint : entrypoints) {
            if (entrypoint.getAdapter().equals("allium")) {
                hasMain = true;
                break;
            }
        }

        this.metadata = metadata;
        this.hasMain = hasMain;
    }


    @Override
    public boolean hasMainAlliumEntrypoint() {
        return hasMain;
    }

    @Override
    public String getMainAlliumEntrypoint() {
        for (EntrypointMetadata entrypoint : metadata.getEntrypoints("main")) {
            if (entrypoint.getAdapter().equals("allium")) {
                return entrypoint.getValue();
            }
        }
        return null;
    }


}

package dev.hugeblank.allium.api;

import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.Registry;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;

public abstract class PlatformHandler implements Identifiable {
    private static final Registry<MappingReference> REGISTRY = new Registry<>();

    public final Mappings getOrCreate() throws IOException {
        if (!REGISTRY.has(getID())) {
            Mappings mappings = new Mappings(createMappingTree(), getDestNS());
            REGISTRY.register(new MappingReference(getID(), mappings));
        }
        return REGISTRY.get(getID()).mappings();
    }

    protected abstract VisitableMappingTree createMappingTree() throws IOException;

    public abstract String getSourceNS();

    public abstract String getDestNS();

    record MappingReference(String getID, Mappings mappings) implements Identifiable {}
}

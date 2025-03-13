package dev.hugeblank.allium.loader;

public record Manifest(String id, String version, String name, String mappings, Entrypoint entrypoints) {

    public boolean isComplete() {
        return !(id == null || version == null || name == null || mappings == null || entrypoints == null);
    }
}

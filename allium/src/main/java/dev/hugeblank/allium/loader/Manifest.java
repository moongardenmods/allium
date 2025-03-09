package dev.hugeblank.allium.loader;

public record Manifest(String id, String version, String name, AlliumEntrypointContainer entrypoints) {

    public boolean isComplete() {
        return !(id == null || version == null || name == null || entrypoints == null);
    }
}

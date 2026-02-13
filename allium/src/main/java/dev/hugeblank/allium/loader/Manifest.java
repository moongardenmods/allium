package dev.hugeblank.allium.loader;

public abstract class Manifest {
    private final String id;
    private final String version;
    private final String name;

    public Manifest(String id, String version, String name) {
        this.id = id;
        this.version = version;
        this.name = name;
    }

    public String id() { return id; }

    public String version() { return version; }

    public String name() { return name; }

    public abstract boolean hasMainAlliumEntrypoint();

    public abstract String getMainAlliumEntrypoint();
}

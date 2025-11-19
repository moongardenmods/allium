package dev.hugeblank.bouquet.util;

import dev.hugeblank.bouquet.BouquetModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

public class BouquetPackResources extends AbstractPackResources {
    public BouquetPackResources(PackLocationInfo location) {
        super(location);
    }

    @Override
    public @Nullable IoSupplier<@NotNull InputStream> getRootResource(String @NotNull ... path) {
        if (path[0].equals("pack.mcmeta")) {
            return () -> Objects.requireNonNull(BouquetModInitializer.class.getResourceAsStream("/assets/pack.mcmeta"));
        } else if (path[0].equals("pack.png")) {
            return () -> Objects.requireNonNull(BouquetModInitializer.class.getResourceAsStream("/assets/bouquet/icon.png"));
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<@NotNull InputStream> getResource(@NotNull PackType type, @NotNull Identifier location) {
        return null;
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String directory, @NotNull ResourceOutput output) {}

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return Set.of();
    }

    @Override
    public void close() {}
}

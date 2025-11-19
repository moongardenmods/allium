package dev.hugeblank.bouquet.util;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.Optional;
import java.util.function.Consumer;

public class ScriptRepositorySource implements RepositorySource {

    @Override
    public void loadPacks(Consumer<Pack> result) {
        PackHelper helper = new PackHelper();
        result.accept(new Pack(
                new PackLocationInfo(
                        Allium.ID,
                        Component.literal("Allium"),
                        PackSource.BUILT_IN,
                        Optional.empty()
                ),
                helper.createPack(),
                new Pack.Metadata(
                        Component.literal("Resources added by Allium scripts"),
                        PackCompatibility.COMPATIBLE,
                        FeatureFlagSet.of(),
                        helper.getScriptsWithPacks().stream().map(Script::getID).toList()
                ),
                new PackSelectionConfig(true, Pack.Position.TOP, true)
        ));
    }
}

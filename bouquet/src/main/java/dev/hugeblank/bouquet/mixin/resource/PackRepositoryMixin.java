package dev.hugeblank.bouquet.mixin.resource;

import dev.hugeblank.bouquet.util.ScriptRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void init(RepositorySource[] sources, CallbackInfo ci) {
        // TODO: Reimplement Resource Pack Library
        this.sources = new HashSet<>(this.sources);
        this.sources.add(new ScriptRepositorySource());
    }
}

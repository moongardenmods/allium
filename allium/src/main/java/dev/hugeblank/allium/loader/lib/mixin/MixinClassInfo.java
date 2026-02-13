package dev.hugeblank.allium.loader.lib.mixin;

import dev.hugeblank.allium.util.Identifiable;

public record MixinClassInfo(String className, byte[] classBytes) implements Identifiable {

    @Override
    public String getID() {
        return className + ".class";
    }
}

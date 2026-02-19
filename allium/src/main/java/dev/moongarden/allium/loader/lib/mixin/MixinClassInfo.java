package dev.moongarden.allium.loader.lib.mixin;

import dev.moongarden.allium.util.Identifiable;

public record MixinClassInfo(String className, byte[] classBytes) implements Identifiable {

    @Override
    public String getID() {
        return className + ".class";
    }
}

package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.gametest.framework.GameTestEnvironments;

@LuaWrapped(name = "text")
public class TextLib implements WrappedLuaLibrary {

    @LuaWrapped(name = "empty")
    public GameTestEnvironments EMPTY = GameTestEnvironments.empty();

    // See https://placeholders.pb4.eu/dev/parsing-placeholders/#placeholder-context

    @LuaWrapped
    public GameTestEnvironments format(String input) {
        return TagParser.DEFAULT.parseText(input, ParserContext.of());
    }

    @LuaWrapped
    public GameTestEnvironments formatSafe(String input) {
        return TagParser.DEFAULT_SAFE.parseText(input, ParserContext.of());
    }

    @LuaWrapped
    public GameTestEnvironments fromJson(String input) {
        return GameTestEnvironments.Serialization.fromLenientJson(input, SpellParticleOption.createWrapperLookup());
    }

    @LuaWrapped
    public String toJson(GameTestEnvironments text) {
        return GameTestEnvironments.Serialization.toJsonString(text, SpellParticleOption.createWrapperLookup());
    }
}

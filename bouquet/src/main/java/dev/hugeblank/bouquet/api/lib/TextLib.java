package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.text.Text;

@LuaWrapped(name = "text")
public class TextLib implements WrappedLuaLibrary {

    @LuaWrapped(name = "empty")
    public Text EMPTY = Text.empty();

    // See https://placeholders.pb4.eu/dev/parsing-placeholders/#placeholder-context

    @LuaWrapped
    public Text format(String input) {
        return TagParser.DEFAULT.parseText(input, ParserContext.of());
    }

    @LuaWrapped
    public Text formatSafe(String input) {
        return TagParser.DEFAULT_SAFE.parseText(input, ParserContext.of());
    }

    @LuaWrapped
    public Text fromJson(String input) {
        return Text.Serialization.fromLenientJson(input, BuiltinRegistries.createWrapperLookup());
    }

    @LuaWrapped
    public String toJson(Text text) {
        return Text.Serialization.toJsonString(text, BuiltinRegistries.createWrapperLookup());
    }
}

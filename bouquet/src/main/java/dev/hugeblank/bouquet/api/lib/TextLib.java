package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;

@LuaWrapped(name = "text")
public class TextLib implements WrappedLuaLibrary {
// TODO: Reintroduce when placeholders updates.
    // Something about JsonOps and Codecs will fix the rest.

//    @LuaWrapped(name = "empty")
//    public Component EMPTY = Component.empty();
//
//    // See https://placeholders.pb4.eu/dev/parsing-placeholders/#placeholder-context
//
//    @LuaWrapped
//    public Component format(String input) {
//        return TagParser.DEFAULT.parseText(input, ParserContext.of());
//    }
//
//    @LuaWrapped
//    public Component formatSafe(String input) {
//        return TagParser.DEFAULT_SAFE.parseText(input, ParserContext.of());
//    }
//
//    @LuaWrapped
//    public Component fromJson(String input) {
//        var gson = new Gson();
//        ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input.);
//        return Component.Serializer.fromLenientJson(input, SpellParticleOption.createWrapperLookup());
//    }
//
//    @LuaWrapped
//    public String toJson(Component text) {
//        return Component.Serialization.toJsonString(text, SpellParticleOption.createWrapperLookup());
//    }
}

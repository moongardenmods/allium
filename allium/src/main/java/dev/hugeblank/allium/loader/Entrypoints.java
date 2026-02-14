package dev.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.Map;

@JsonAdapter(Entrypoints.Adapter.class)
public record Entrypoints(@Expose(deserialize = false) Map<Type, String> initializers) {

    public boolean valid() {
        return initializers.containsKey(Type.MAIN);
    }

    public boolean has(Type t) {
        return initializers.containsKey(t);
    }

    public String get(Type t) {
        return initializers.get(t);
    }

    public enum Type {
        MAIN("main"),
        MIXIN("mixin");

        private final String key;

        Type(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }

    public static class Adapter implements JsonDeserializer<Entrypoints> {

        @Override
        public Entrypoints deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            final Map<Type, String> initializers = new HashMap<>();
            for (Type type : Type.values()) {
                if (json.has(type.getKey())) {
                    initializers.put(type, json.get(type.getKey()).getAsString());
                }
            }
            return new Entrypoints(initializers);
        }
    }
}

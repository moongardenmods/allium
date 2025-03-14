package dev.hugeblank.allium.loader;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.Map;

@JsonAdapter(Entrypoint.Adapter.class)
public class Entrypoint {
    @Expose(deserialize = false)
    private final Map<Type, String> initializers;

    public Entrypoint(Map<Type, String> initializers) {
        this.initializers = initializers;
    }

    public boolean valid() {
        return initializers.containsKey(Type.STATIC) || initializers.containsKey(Type.DYNAMIC);
    }

    public boolean has(Type t) {
        return initializers.containsKey(t);
    }

    public String get(Type t) {
        return initializers.get(t);
    }

    public enum Type {
        STATIC("static"),
        DYNAMIC("dynamic");

        private final String key;
        Type(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static class Adapter implements JsonDeserializer<Entrypoint> {

        @Override
        public Entrypoint deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            final Map<Type, String> initializers = new HashMap<>();
            for (Type type : Type.values()) {
                if (json.has(type.getKey())) {
                    initializers.put(type, json.get(type.getKey()).getAsString());
                }
            }
            return new Entrypoint(initializers);
        }
    }
}

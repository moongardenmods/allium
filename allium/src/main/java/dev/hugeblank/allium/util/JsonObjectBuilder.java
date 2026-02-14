package dev.hugeblank.allium.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonObjectBuilder {
    private final JsonObject object = new JsonObject();

    public JsonObjectBuilder add(String key, String value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder add(String key, Boolean value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder add(String key, Number value) {
        object.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder add(String key, JsonElement value) {
        object.add(key, value);
        return this;
    }

    public JsonObject build() {
        return object;
    }

    public static JsonObjectBuilder of() {
        return new JsonObjectBuilder();
    }
}

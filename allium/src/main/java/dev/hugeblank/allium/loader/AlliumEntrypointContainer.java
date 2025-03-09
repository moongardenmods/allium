package dev.hugeblank.allium.loader;


import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;

import java.util.HashMap;
import java.util.Map;

@JsonAdapter(AlliumEntrypointContainer.Adapter.class)
public class AlliumEntrypointContainer {
    @Expose(deserialize = false)
    private final Map<ScriptRegistry.EnvType, Entrypoint> entrypoints;

    public AlliumEntrypointContainer(Map<ScriptRegistry.EnvType, Entrypoint> entrypoints) {
        this.entrypoints = entrypoints;
    }

    public boolean has(ScriptRegistry.EnvType t) {
        return  entrypoints.containsKey(t);
    }

    public Entrypoint get(ScriptRegistry.EnvType t) {
        return entrypoints.get(t);
    }

    public static class Adapter implements JsonDeserializer<AlliumEntrypointContainer> {

        @Override
        public AlliumEntrypointContainer deserialize(JsonElement element, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            final Map<ScriptRegistry.EnvType, Entrypoint> entrypoints = new HashMap<>();
            for (ScriptRegistry.EnvType envType : ScriptRegistry.EnvType.values()) {
                if (json.has(envType.getKey())) {
                    entrypoints.put(envType, context.deserialize(json.get(envType.getKey()), Entrypoint.class));
                }
            }
            return new AlliumEntrypointContainer(entrypoints);
        }
    }


}

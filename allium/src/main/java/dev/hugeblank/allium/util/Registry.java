package dev.hugeblank.allium.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Registry<T extends Identifiable> {

    protected final Map<String, T> registry = new HashMap<>();

    protected void onRegister(T value) {};

    public void register(T value) {
        String id = value.getID();
        if (!has(id)) {
            registry.put(id, value);
            onRegister(value);
        } else {
            throw new RuntimeException("ID " + id + " already registered!");
        }
    }

    public boolean has(String id) {
        return registry.containsKey(id);
    }

    public T get(String id) {
        return registry.get(id);
    }

    public Set<T> getAll() {
        return new HashSet<>(registry.values());
    }

    public void forEach(Consumer<T> callback) {
        registry.forEach((id, value) -> callback.accept(value));
    }
}

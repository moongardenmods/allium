package dev.hugeblank.allium.api;

import dev.hugeblank.allium.util.Identifiable;

import java.util.Map;

public interface MappingsLoader extends Identifiable {
    Map<String, String> load();
}

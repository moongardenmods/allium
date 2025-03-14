package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.Registry;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Debug;

import java.util.*;

@Debug.Renderer(text = "\"Mappings { ... }\"", hasChildren = "false")
public record Mappings(String getID, Map<String, List<String>> mapped2unmapped, Map<String, String> unmapped2mapped) implements Identifiable {
    public static final Registry<MappingsLoader> LOADERS = new Registry<>();
    public static final Registry<Mappings> REGISTRY = new Registry<>();

    public static Mappings of(String id, Map<String, String> unmapped2mapped) {
        var mapped2unmapped = new HashMap<String, List<String>>();

        for (var entry : unmapped2mapped.entrySet()) {
            mapped2unmapped.computeIfAbsent(entry.getValue(), (s) -> new ArrayList<>()).add(entry.getKey());
        }

        return new Mappings(id, mapped2unmapped, unmapped2mapped);
    }

    public List<String> getUnmapped(String value) {
        var val = this.mapped2unmapped.get(value);
        return val != null && !Allium.DEVELOPMENT ? val : List.of(value);
    }

    public String getMapped(String value) {
        return this.unmapped2mapped.getOrDefault(value, value);
    }

    public static String asMethod(String className, String method) {
        return (className + "#" + method).replace('/', '.');
    }

    public static String asMethod(EClass<?> clazz, EMethod method) {
        return asMethod(clazz.name(), method.name());
    }

    public static String asMethod(EClass<?> clazz, EField field) {
        return asMethod(clazz.name(), field.name());
    }

    public static String asClass(String className) {
        return className.replace('/', '.');
    }

    public static String asClass(EClass<?> clazz) {
        return asClass(clazz.name());
    }
}

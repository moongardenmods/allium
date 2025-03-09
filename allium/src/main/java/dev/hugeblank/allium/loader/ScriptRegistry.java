package dev.hugeblank.allium.loader;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ScriptRegistry {
    public static final ScriptRegistry COMMON;
    public static final ScriptRegistry CLIENT;
    public static final ScriptRegistry SERVER;

    private static final Map<net.fabricmc.api.EnvType, EnvType> TYPEMAP = new HashMap<>();

    private final EnvType envType;
    private final Map<String, Script> scripts;

    private ScriptRegistry(EnvType envType) {
        this.envType = envType;
        this.scripts = new HashMap<>();
    }

    public void reloadAll() {
        getInstance(envType).forEach(Script::reload);
    }

    public void registerScript(Script script) {
        String id = script.getId();
        if (!scripts.containsKey(id)) {
            scripts.put(id, script);
        } else {
            throw new RuntimeException("Script with ID is already loaded!");
        }
    }

    public void unloadScript(String name) {
        if (scripts.containsKey(name)) {
            scripts.get(name).unload();
        }
    }

    public EnvType getType() {
        return envType;
    }

    public boolean hasScript(String name) {
        return scripts.containsKey(name);
    }

    public Script getScript(String name) {
        return scripts.get(name);
    }

    public void forEach(Consumer<Script> callback) {
        scripts.forEach((id, script) -> callback.accept(script));
    }

    public Set<Script> getScripts() {
        return Sets.newHashSet(scripts.values());
    }

    public static ScriptRegistry getInstance(EnvType type) {
        return switch (type) {
            case SERVER -> SERVER;
            case COMMON -> COMMON;
            case CLIENT -> CLIENT;
        };
    }

    static {
        COMMON = new ScriptRegistry(EnvType.COMMON);
        CLIENT = new ScriptRegistry(EnvType.CLIENT);
        SERVER = new ScriptRegistry(EnvType.SERVER);
    }

    public enum EnvType {
        COMMON("common", null), // common & server code
        CLIENT("client", net.fabricmc.api.EnvType.CLIENT), // client code only
        SERVER("server", net.fabricmc.api.EnvType.SERVER); // server code only

        private final String key;
        EnvType(String key, net.fabricmc.api.EnvType type) {
            this.key = key;
            TYPEMAP.put(type, this);
        }

        public String getKey() {
            return key;
        }

        public static EnvType unwrap(net.fabricmc.api.EnvType type) {
            return TYPEMAP.get(type);
        }
    }
}

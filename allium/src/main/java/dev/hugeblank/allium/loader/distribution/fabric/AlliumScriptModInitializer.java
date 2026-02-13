package dev.hugeblank.allium.loader.distribution.fabric;

import dev.hugeblank.allium.loader.Script;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;

import java.util.List;

public class AlliumScriptModInitializer implements ModInitializer {
    private final Script script;
    private final String target;

    public AlliumScriptModInitializer(Script script, String target) {
        this.script = script;
        this.target = target;
    }

    @Override
    public void onInitialize() {
        if (AlliumLanguageAdapter.LAUNCH_ORDER.containsKey(script)) {
            for (Script dep : AlliumLanguageAdapter.LAUNCH_ORDER.get(script)) {
                ModContainer mod = FabricLoader.getInstance().getModContainer(script.getID()).orElseThrow();
                if (mod.getMetadata() instanceof LoaderModMetadata metadata) {
                    List<EntrypointMetadata> entrypoints = metadata.getEntrypoints("main");
                    for (EntrypointMetadata entrypoint : entrypoints) {
                        if (entrypoint.getAdapter().equals("allium")) dep.initialize(entrypoint.getValue());
                    }
                }
                if (dep.getLaunchState() != Script.State.INITIALIZED) {
                    script.getLogger().error("Failed to initialize dependency '{}'.", dep.getID());
                }
                // If there is a world where a built-in mod uses allium, I'll eat my shoe.
            }
        }
        script.initialize(target);
    }
}

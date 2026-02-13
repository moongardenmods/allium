package dev.hugeblank.allium.loader.distribution.fabric;

import dev.hugeblank.allium.loader.Script;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.Map;
import java.util.Set;

public class AlliumScriptPreLaunchEntrypoint implements PreLaunchEntrypoint {
    private final Script script;
    private final String target;
    public AlliumScriptPreLaunchEntrypoint(Script script, String target) {
        this.script = script;
        this.target = target;
    }

    @Override
    public void onPreLaunch() {
        script.preInitialize(target);
    }
}

package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import dev.moongarden.allium.api.ScriptResource;
import dev.moongarden.allium.api.event.MixinMethodHook;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@LuaWrapped
public class HookDefinition {
    private final Map<String, MixinMethodHook> hooks;

    public HookDefinition(Map<String, MixinMethodHook> hooks) {
        this.hooks = hooks;
    }

    // Used in bytecode.
    public MixinMethodHook forId(String id) {
        return hooks.get(id);
    }

    @LuaWrapped
    public ScriptResource define(LuaTable table, @OptionalArg @Nullable Boolean destroyOnUnload) throws LuaError {
        List<ScriptResource> resources = new ArrayList<>();
        for (MixinMethodHook hook : hooks.values()) {
            resources.add(hook.hook(table.rawget(hook.id()).checkFunction(), destroyOnUnload));
        }
        return () -> {
            for (ScriptResource resource : resources) resource.close();
        };
    }
}

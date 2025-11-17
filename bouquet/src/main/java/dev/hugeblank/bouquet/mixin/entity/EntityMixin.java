package dev.hugeblank.bouquet.mixin.entity;

import dev.hugeblank.bouquet.api.event.CommonEvents;
import dev.hugeblank.bouquet.util.EntityDataHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public class EntityMixin implements EntityDataHolder {
    @Unique
    private final Map<String, LuaValue> allium_tempData = new HashMap<>();

    @Override
    public LuaValue allium$getTemporaryData(String key) {
        return this.allium_tempData.getOrDefault(key, Constants.NIL);
    }

    @Override
    public void allium$setTemporaryData(String key, LuaValue value) {
        this.allium_tempData.put(key, value);
    }


    @Override
    public void allium_private$copyTempData(Entity source) {
        var mixined = (EntityMixin) (Object) source;
        this.allium_tempData.clear();
        if (mixined != null) {
            this.allium_tempData.putAll(mixined.allium_tempData);
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void save(ValueOutput valueOutput, CallbackInfoReturnable<ValueOutput> cir) {
        CommonEvents.ENTITY_SAVE.invoker().save(valueOutput);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void load(ValueInput valueInput, CallbackInfo ci) {
        CommonEvents.ENTITY_LOAD.invoker().load(valueInput);
    }
}

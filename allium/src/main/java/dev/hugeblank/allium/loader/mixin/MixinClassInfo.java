package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.MixinConfigUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

public record MixinClassInfo(String className, byte[] classBytes, boolean isDuck) implements Identifiable {

    @LuaWrapped
    public LuaValue quack() throws ClassNotFoundException {
        if (!MixinConfigUtil.isComplete())
            throw new IllegalStateException("Mixin cannot be accessed in pre-launch phase.");
        if (!isDuck) throw new IllegalStateException("Cannot get duck interface of non-interface mixin.");
        EClass<?> clazz = EClass.fromJava(Class.forName(className));
        return StaticBinder.bindClass(clazz);
    }

    @Override
    public String getID() {
        return className + ".class";
    }
}

package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.MixinConfigUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

public class MixinClassInfo implements Identifiable {

    private final String className;
    private final byte[] classBytes;
    private final boolean isDuck;

    public MixinClassInfo(String className, byte[] classBytes, boolean isDuck) {
        this.className = className.replace("/", ".");
        this.classBytes = classBytes;
        this.isDuck = isDuck;
    }

    public byte[] getBytes() {
        return classBytes;
    }

    @LuaWrapped
    public LuaValue quack() throws ClassNotFoundException, LuaError {
        if (!MixinConfigUtil.isComplete()) throw new IllegalStateException("Mixin cannot be accessed in pre-launch phase.");
        if (!isDuck) throw new IllegalStateException("Cannot get duck interface of non-interface mixin.");
        EClass<?> clazz = EClass.fromJava(Class.forName(className));
        return StaticBinder.bindClass(clazz);
    }

    @Override
    public String getID() {
        return className + ".class";
    }
}

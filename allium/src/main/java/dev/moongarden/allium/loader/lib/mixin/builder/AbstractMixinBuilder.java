package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.MixinLib;
import dev.moongarden.allium.loader.lib.clazz.builder.AbstractClassBuilder;
import dev.moongarden.allium.loader.lib.mixin.MixinClassInfo;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.AsmUtil;
import dev.moongarden.allium.util.asm.VisitedClass;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;

import java.util.regex.Matcher;

public class AbstractMixinBuilder extends AbstractClassBuilder {
    protected final VisitedClass visitedClass;
    protected final @Nullable EnvType targetEnvironment;
    protected final Script script;

    public static AbstractMixinBuilder create(String target, String[] interfaces, @Nullable EnvType targetEnvironment, boolean duck, Script script) throws LuaError {
        checkPhase();
        VisitedClass visitedClass = VisitedClass.ofClass(target);
        return duck ? new MixinInterfaceBuilder(
            visitedClass,
            interfaces,
            targetEnvironment,
            script
        ) : new MixinClassBuilder(
            visitedClass,
            interfaces,
            targetEnvironment,
            script
        );
    }

    public AbstractMixinBuilder(VisitedClass visitedClass, int access, String[] interfaces, @Nullable EnvType targetEnvironment, Script script) {
        super(
            script.getMixinLib().getUniqueMixinClassName(),
            EClass.fromJava(Object.class).name().replace('.', '/'),
            interfaces,
            access | visitedClass.access(),
            visitedClass.signature()
        );
        this.visitedClass = visitedClass;
        this.targetEnvironment = targetEnvironment;
        this.script = script;
    }

    public void build(String id) throws InvalidMixinException, LuaError {
        c.visitEnd();
        byte[] classBytes = c.toByteArray();
        AsmUtil.dumpClass(className, classBytes);

        // give the class back to the user for later use in the case of an interface.
        MixinClassInfo info = new MixinClassInfo(className.replace("/", "."), classBytes);

        script.getMixinLib().addClassInfo(info, targetEnvironment);
    }

    protected static void checkPhase() {
        if (MixinLib.isComplete())
            throw new IllegalStateException("Mixins cannot be created outside of preLaunch phase.");
    }

    public static String cleanDescriptor(VisitedClass visitedClass, String descriptor) {
        String classType = visitedClass.getType().getDescriptor();
        if (descriptor.startsWith(classType))
            descriptor = descriptor.replaceFirst(Matcher.quoteReplacement(classType), "");
        return descriptor;
    }
}

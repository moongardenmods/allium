package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.Allium;
import dev.moongarden.allium.api.event.MixinMethodHook;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.InjectorChef;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.LuaMethodAnnotation;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.*;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.squiddev.cobalt.LuaError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/* Notes on implementation:
 - Accessors & Invokers MUST have target name set as the value in the annotation. We use that to determine which
 method/field to invoke/access
*/

@LuaWrapped
public class MixinClassBuilder extends AbstractMixinBuilder {
    private final List<ChefHolder> kitchen = new ArrayList<>();
    private final Map<String, MixinMethodHook> hooks = new HashMap<>();

    public MixinClassBuilder(VisitedClass visitedClass, String[] interfaces, @Nullable EnvType targetEnvironment, Script script) {
        super(
            visitedClass,
            ACC_PUBLIC,
            interfaces,
            targetEnvironment,
            script
        );
        Allium.PROFILER.push(script.getID(), "mixin", visitedClass.name());

        AnnotationVisitor mixinAnnotation = this.c.visitAnnotation(Mixin.class.descriptorString(), false);
        AnnotationVisitor targetArray = mixinAnnotation.visitArray("value");
        targetArray.visit(null, Type.getObjectType(visitedClass.name()));
        targetArray.visitEnd();
        mixinAnnotation.visitEnd();
        Allium.PROFILER.pop();
    }

    @LuaWrapped
    public MixinMethodBuilder method(String index) {
        checkPhase();
        return new MixinMethodBuilder(this, index);
    }

    @LuaWrapped
    public void build(String id) throws InvalidMixinException, LuaError {
        for (ChefHolder holder : kitchen) {
            if (hooks.containsKey(holder.index())) throw new InvalidMixinException(InvalidMixinException.Type.INJECTOR_EXISTS, holder.index());
            hooks.put(holder.index(), holder.chef().bake(script, id, holder.index(), c, visitedClass, holder.annotations(), holder.sugarParameters()));
        }

        // In case the class being mixed into loads, we initialize the script so it has a chance to hook before anything else runs.
        MethodVisitor clinit = c.visitMethod(ACC_PRIVATE|ACC_STATIC, "clinit", "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", null, null);
        AnnotationVisitor inject = clinit.visitAnnotation(Type.getDescriptor(Inject.class), true);

        AnnotationVisitor method = inject.visitArray("method");
        method.visit(null, "<clinit>()V");
        method.visitEnd();
        AnnotationVisitor atArray = inject.visitArray("at");
        AnnotationVisitor at = atArray.visitAnnotation(null, Type.getDescriptor(At.class));
        at.visit("value", "HEAD");
        at.visitEnd();
        atArray.visitEnd();
        inject.visitEnd();
        clinit.visitCode();
        AsmUtil.getScript(clinit, script);
        clinit.visitMethodInsn(INVOKEVIRTUAL, Owners.SCRIPT, "initialize", "()V", false);
        clinit.visitInsn(RETURN);
        clinit.visitEnd();

        super.build(id);

        script.getMixinLib().addHooks(id, hooks);
    }

    public void prepareMethod(
        InjectorChef chef,
        String index,
        List<LuaMethodAnnotation> annotations,
        List<? extends LuaSugar> sugarParameters
    ) { kitchen.add(new ChefHolder(chef, index, annotations, sugarParameters)); }

    private record ChefHolder(InjectorChef chef, String index, List<LuaMethodAnnotation> annotations, List<? extends LuaSugar> sugarParameters) {}

}


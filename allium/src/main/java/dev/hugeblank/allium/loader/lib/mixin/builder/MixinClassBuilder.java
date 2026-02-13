package dev.hugeblank.allium.loader.lib.mixin.builder;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.builder.AbstractClassBuilder;
import dev.hugeblank.allium.loader.lib.mixin.MixinClassInfo;
import dev.hugeblank.allium.loader.lib.mixin.annotation.LuaAnnotationParser;
import dev.hugeblank.allium.loader.lib.mixin.annotation.method.InjectorChef;
import dev.hugeblank.allium.loader.lib.mixin.annotation.method.LuaMethodAnnotation;
import dev.hugeblank.allium.loader.lib.mixin.annotation.sugar.LuaSugar;
import dev.hugeblank.allium.api.annotation.LuaWrapped;
import dev.hugeblank.allium.api.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.exception.InvalidMixinException;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.Registry;
import dev.hugeblank.allium.util.asm.AsmUtil;
import dev.hugeblank.allium.util.asm.VisitedClass;
import dev.hugeblank.allium.util.asm.VisitedField;
import dev.hugeblank.allium.util.asm.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.objectweb.asm.Opcodes.*;

/* Notes on implementation:
 - Accessors & Invokers MUST have target name set as the value in the annotation. We use that to determine which
 method/field to invoke/access
*/

@LuaWrapped
public class MixinClassBuilder extends AbstractClassBuilder {
    public static final List<MixinClassInfo> MIXINS = new ArrayList<>();
    public static final List<MixinClassInfo> CLIENT = new ArrayList<>();
    public static final List<MixinClassInfo> SERVER = new ArrayList<>();

    private final EnvType targetEnvironment;
    private final boolean duck;
    private final VisitedClass visitedClass;
    private final Script script;

    public static MixinClassBuilder create(String target, String[] interfaces, @Nullable EnvType targetEnvironment, boolean duck, Script script) throws LuaError {
        checkPhase();
        return new MixinClassBuilder(
                VisitedClass.ofClass(target),
                interfaces,
                targetEnvironment,
                duck,
                script
        );
    }

    private MixinClassBuilder(VisitedClass visitedClass, String[] interfaces, @Nullable EnvType targetEnvironment, boolean duck, Script script) {
        super(
                AsmUtil.getUniqueMixinClassName(),
                EClass.fromJava(Object.class).name().replace('.', '/'),
                interfaces,
                ACC_PUBLIC | (duck ? ACC_ABSTRACT | ACC_INTERFACE : 0) | visitedClass.access(),
                visitedClass.signature()
        );
        Allium.PROFILER.push(script.getID(), "mixin", visitedClass.name());
        this.script = script;
        this.targetEnvironment = targetEnvironment;
        this.duck = duck;
        this.visitedClass = visitedClass;

        AnnotationVisitor mixinAnnotation = this.c.visitAnnotation(Mixin.class.descriptorString(), false);
        AnnotationVisitor targetArray = mixinAnnotation.visitArray("value");
        targetArray.visit(null, Type.getObjectType(visitedClass.name()));
        targetArray.visitEnd();
        mixinAnnotation.visitEnd();
        Allium.PROFILER.pop();
    }

    @LuaWrapped
    public void createInjectMethod(String hookId, List<LuaMethodAnnotation> methodAnnotations, @OptionalArg @Nullable List<? extends LuaSugar> sugarParameters) throws InvalidMixinException, InvalidArgumentException, LuaError {
        checkPhase();
        Allium.PROFILER.push("createInjectMethod", hookId);
        if (visitedClass.isInterface() || this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "class");

        String eventId = script.getID() + ':' + hookId;

        List<InjectorChef> chefList = methodAnnotations.stream()
                .filter((methodAnnotation) -> methodAnnotation instanceof InjectorChef)
                .map(ma -> (InjectorChef) ma).toList();
        if (chefList.isEmpty()) {
            throw new InvalidMixinException(InvalidMixinException.Type.NO_INJECTOR_ANNOTATION, eventId);
        } else if (chefList.size() > 1) {
            throw new InvalidMixinException(InvalidMixinException.Type.TOO_MANY_INJECTOR_ANNOTATIONS, eventId);
        }
        chefList.getFirst().bake(script, eventId, c, visitedClass, methodAnnotations, sugarParameters);
        Allium.PROFILER.pop();
    }

    @LuaWrapped
    public void accessor(LuaTable annotations) throws InvalidArgumentException, InvalidMixinException, LuaError {
        // Shorthand method for writing both setter and getter accessor methods
        setAccessor(annotations);
        getAccessor(annotations);
    }

    @LuaWrapped
    public void setAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(true, annotations);
    }

    @LuaWrapped
    public void getAccessor(LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(false, annotations);
    }

    private void writeAccessor(boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        if (!this.duck)
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "interface");
        String descriptor = getTargetValue(annotations);
        // TODO: if the descriptor starts with the class name remove it!
        if (visitedClass.containsField(descriptor)) {
            VisitedField visitedField = visitedClass.getField(descriptor);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            String name = visitedField.name();
            name = (isSetter ? "set" : "get") + // set or get
                    name.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    name.substring(1); // Rest of name
            List<MixinParameter> params = isSetter ? List.of(new MixinParameter(visitedFieldType)) : List.of();

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedField, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotationParser(
                    script.getState(),
                    annotations,
                    EClass.fromJava(Accessor.class)
            )));

            if (visitedField.needsInstance()) {
                methodBuilder.code((visitor, _, _) -> {
                    AsmUtil.visitObjectDefinition(
                            visitor,
                            Type.getInternalName(UnsupportedOperationException.class),
                            "()V"
                    ).run();
                    visitor.visitInsn(ATHROW);
                    visitor.visitMaxs(0, 0);
                });
            }

            methodBuilder
                    .access(visitedField.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT))
                    .returnType(isSetter ? Type.VOID_TYPE : visitedFieldType)
                    .signature(visitedField.signature())
                    .build();
        }
    }


    @LuaWrapped
    public void invoker(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        checkPhase();
        String methodName = getTargetValue(annotations);
        if (visitedClass.containsMethod(methodName)) {
            VisitedMethod visitedMethod = visitedClass.getMethod(methodName);
            String name = visitedMethod.name();
            name = "invoke" +
                    name.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                    name.substring(1);// Rest of name

            List<MixinParameter> params = Arrays.stream(
                    Type.getArgumentTypes(visitedMethod.descriptor())).map(MixinParameter::new
            ).toList();

            MixinMethodBuilder methodBuilder = MixinMethodBuilder.of(c, visitedMethod, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotationParser(
                    script.getState(),
                    annotations,
                    EClass.fromJava(Invoker.class)
            )));

            if (visitedMethod.needsInstance()) {
                methodBuilder.code((visitor, _, _) -> {
                    AsmUtil.visitObjectDefinition(visitor, Type.getInternalName(AssertionError.class), "()V").run();
                    visitor.visitInsn(ATHROW);
                    visitor.visitMaxs(0,0);
                });
            }

            methodBuilder
                    .access(visitedMethod.needsInstance() ? ACC_PUBLIC : (ACC_PUBLIC|ACC_ABSTRACT))
                    .returnType(Type.getReturnType(visitedMethod.descriptor()))
                    .signature(visitedMethod.signature())
                    .exceptions(visitedMethod.exceptions())
                    .build();
        }
    }

    private String getTargetValue(LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        if (!visitedClass.isInterface())
            throw new InvalidMixinException(InvalidMixinException.Type.INVALID_CLASSTYPE, "interface");
        String name = null;
        if (annotations.rawget("value").isString()) {
            name = annotations.rawget("value").checkString();
        } else if (annotations.rawget(1).isString()) {
            name = annotations.rawget(1).checkString();
        }
        if (name == null) {
            throw new InvalidArgumentException("Expected field name at key 'value' or index 1");
        } else {
            return name;
        }
    }


    private static void checkPhase() {
        if (MixinConfigUtil.isComplete())
            throw new IllegalStateException("Mixins cannot be created outside of preLaunch phase.");
    }

    @LuaWrapped
    public void build(@OptionalArg String id) throws LuaError {
        if (!duck) {
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
            clinit.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ScriptRegistry.class), "getInstance", "()Ldev/hugeblank/allium/loader/ScriptRegistry;", false);
            clinit.visitLdcInsn(script.getID());
            clinit.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Registry.class), "get", "(Ljava/lang/String;)Ldev/hugeblank/allium/util/Identifiable;", false);
            String scriptName = Type.getInternalName(Script.class);
            clinit.visitTypeInsn(CHECKCAST, scriptName);
            clinit.visitMethodInsn(INVOKEVIRTUAL, scriptName, "initialize", "()V", false);
            clinit.visitInsn(RETURN);
            clinit.visitEnd();
        }

        c.visitEnd();
        byte[] classBytes = c.toByteArray();
        AsmUtil.dumpClass(className, classBytes);

        // give the class back to the user for later use in the case of an interface.
        MixinClassInfo info = new MixinClassInfo(className.replace("/", "."), classBytes);

        if (duck) {
            if (id == null) throw new LuaError("Missing 'id' parameter for duck mixin on " + className);
            MixinLib.DUCK_MAP.put(script.getID() + ':' + id, className);
        }

        List<MixinClassInfo> envList = (targetEnvironment == null) ? MIXINS : switch (targetEnvironment) {
            case SERVER -> SERVER;
            case CLIENT -> CLIENT;
        };
        envList.add(info);
    }

}


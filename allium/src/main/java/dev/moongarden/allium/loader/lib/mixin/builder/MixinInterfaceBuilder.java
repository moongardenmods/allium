package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.api.LuaStateArg;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.loader.Script;
import dev.moongarden.allium.loader.lib.mixin.annotation.LuaAnnotationParser;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import dev.moongarden.allium.util.asm.AsmUtil;
import dev.moongarden.allium.util.asm.VisitedClass;
import dev.moongarden.allium.util.asm.VisitedField;
import dev.moongarden.allium.util.asm.VisitedMethod;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.objectweb.asm.Opcodes.*;

public class MixinInterfaceBuilder extends AbstractMixinBuilder {
    public MixinInterfaceBuilder(VisitedClass visitedClass, String[] interfaces, @Nullable EnvType targetEnvironment, Script script) {
        super(visitedClass, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, interfaces, targetEnvironment, script);

    }

    @LuaWrapped
    public MixinInterfaceBuilder accessor(@LuaStateArg LuaState state, LuaTable annotations) throws InvalidArgumentException, InvalidMixinException, LuaError {
        // Shorthand method for writing both setter and getter accessor methods
        setAccessor(state, annotations);
        getAccessor(state, annotations);
        return this;
    }

    @LuaWrapped
    public MixinInterfaceBuilder setAccessor(@LuaStateArg LuaState state, LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(state, true, annotations);
        return this;
    }

    @LuaWrapped
    public MixinInterfaceBuilder getAccessor(@LuaStateArg LuaState state, LuaTable annotations) throws LuaError, InvalidArgumentException, InvalidMixinException {
        checkPhase();
        writeAccessor(state, false, annotations);
        return this;
    }

    private void writeAccessor(@LuaStateArg LuaState state, boolean isSetter, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
        String descriptor = getTargetValue(annotations);
        if (visitedClass.containsField(descriptor)) {
            VisitedField visitedField = visitedClass.getField(descriptor);
            Type visitedFieldType = Type.getType(visitedField.descriptor());
            String name = visitedField.name();
            name = (isSetter ? "set" : "get") + // set or get
                name.substring(0, 1).toUpperCase(Locale.getDefault()) + // Uppercase first letter
                name.substring(1); // Rest of name
            List<MixinParameter> params = isSetter ? List.of(new MixinParameter(visitedFieldType)) : List.of();

            InternalMixinMethodBuilder methodBuilder = new InternalMixinMethodBuilder(c, visitedField, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotationParser(
                state,
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
                .buildForInterface();
        }
    }


    @LuaWrapped
    public MixinInterfaceBuilder invoker(@LuaStateArg LuaState state, LuaTable annotations) throws InvalidMixinException, LuaError, InvalidArgumentException {
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

            InternalMixinMethodBuilder methodBuilder = new InternalMixinMethodBuilder(c, visitedMethod, name, params);

            methodBuilder.annotations(List.of(new LuaAnnotationParser(
                state,
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
                .buildForInterface();
        }
        return this;
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
            return cleanDescriptor(visitedClass, name);
        }
    }

    @Override
    public void build(String id) throws InvalidMixinException, LuaError {
        if (script.getMixinLib().hasDuck(id))
            throw new InvalidMixinException(InvalidMixinException.Type.DUCK_EXISTS, id);

        if (id == null) throw new LuaError("Missing 'id' parameter for duck mixin on " + className);

        script.getMixinLib().addDuck(id, className);

        super.build(id);
    }
}

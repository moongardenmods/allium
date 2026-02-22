package dev.moongarden.allium.loader.lib.mixin.builder;

import dev.moongarden.allium.api.LuaStateArg;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.*;
import dev.moongarden.allium.loader.lib.mixin.annotation.method.injectors.*;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaCancellable;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaLocal;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaParameterAnnotation;
import dev.moongarden.allium.loader.lib.mixin.annotation.sugar.LuaShare;
import dev.moongarden.allium.loader.type.exception.InvalidArgumentException;
import dev.moongarden.allium.loader.type.exception.InvalidMixinException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped
public class MixinMethodBuilder {

    private final MixinClassBuilder classBuilder;
    private final String index;
    private InjectorChef chef;
    private final List<LuaParameterAnnotation> sugars = new ArrayList<>();
    private final List<LuaMethodAnnotation> annotations = new ArrayList<>();

    public MixinMethodBuilder(MixinClassBuilder classBuilder, String index) {
        this.classBuilder = classBuilder;
        this.index = index;
    }

    /// Add a variable that exists within the method to the parameter list.
    /// If `mutable` is set, the parameter becomes a corresponding LocalRef.
    /// @see com.llamalad7.mixinextras.sugar.ref
    @LuaWrapped
    public MixinMethodBuilder localref(@LuaStateArg LuaState state, String type, @OptionalArg @Nullable LuaTable annotation, @OptionalArg @Nullable Boolean mutable) throws InvalidArgumentException, LuaError {
        sugars.add(new LuaLocal(state, type,
            mutable != null && mutable,
            annotation == null ? new LuaTable() : annotation));
        return this;
    }

    /// Add a shared value to the parameter list.
    /// Shared values are preserved across multiple injections on the same method.
    /// @see com.llamalad7.mixinextras.sugar.Share
    @LuaWrapped
    public MixinMethodBuilder share(@LuaStateArg LuaState state, String type, LuaTable annotation) throws InvalidArgumentException, LuaError {
        sugars.add(new LuaShare(state, type, annotation));
        return this;
    }

    /// Add a CallbackInfo or CallbackInfoReturnable to the list of parameters.
    /// Useful if one is not already provided.
    /// @see org.spongepowered.asm.mixin.injection.callback.CallbackInfo
    /// @see org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
    /// @see com.llamalad7.mixinextras.sugar.Cancellable
    @LuaWrapped
    public MixinMethodBuilder cancellable(@LuaStateArg LuaState state) throws InvalidArgumentException, LuaError {
        sugars.add(new LuaCancellable(state));
        return this;
    }

    /// Create a standard @Inject annotation.
    ///
    /// [Mixin Cheatsheet](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/inject.md)
    ///
    /// [Mixin Javadoc](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/Inject.html)
    ///
    /// @see org.spongepowered.asm.mixin.injection.Inject
    @LuaWrapped
    public MixinMethodBuilder inject(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaInject(state, annotation);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create a @ModifyArg annotation.
    ///
    /// [Mixin Cheatsheet](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/modify-arg.md)
    ///
    /// [Mixin Javadoc](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArg.html)
    ///
    /// @param targetType a type descriptor string of the argument being modified.
    /// @see org.spongepowered.asm.mixin.injection.ModifyArg
    @LuaWrapped
    public MixinMethodBuilder modifyArg(@LuaStateArg LuaState state, LuaTable annotation, String targetType) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaModifyArg(state, annotation, targetType);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create a @ModifyArgs annotation.
    ///
    /// [Mixin Cheatsheet](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/modify-args.md)
    ///
    /// [Mixin Javadoc](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArgs.html)
    ///
    /// @see org.spongepowered.asm.mixin.injection.ModifyArgs
    @LuaWrapped
    public MixinMethodBuilder modifyArgs(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaModifyArgs(state, annotation);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create a @ModifyExpressionValue annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue)
    ///
    /// @param targetType a type descriptor string of the argument being modified.
    /// @see com.llamalad7.mixinextras.injector.ModifyExpressionValue
    @LuaWrapped
    public MixinMethodBuilder modifyExpressionValue(@LuaStateArg LuaState state, LuaTable annotation, String targetType) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaModifyExpressionValue(state, annotation, targetType);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create a @ModifyReturnValue annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyReturnValue)
    ///
    /// @see com.llamalad7.mixinextras.injector.ModifyReturnValue
    @LuaWrapped
    public MixinMethodBuilder modifyReturnValue(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaModifyReturnValue(state, annotation);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create a @WrapMethod annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod)
    ///
    /// @see com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
    @LuaWrapped
    public MixinMethodBuilder wrapMethod(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaWrapMethod(state, annotation);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Supply a custom injector annotation that does not have an existing wrapper function.
    ///
    /// Method descriptor, parameter types and return type must be provided using java ASM syntax (ex. Lcom.example.package.ClassName;).
    ///
    /// Due to the inability to infer the expected parameters of many mixins, this function is offered as a catch-all.
    /// One must be incredibly explicit with your descriptor, parameter, and return types, or this will
    /// generate a method that will crash at the mixin apply phase. Here be dragons, you have been warned.
    @LuaWrapped
    public MixinMethodBuilder custom(@LuaStateArg LuaState state, LuaTable annotation, EClass<?> annotationType, String methodDescriptor, List<String> parameterTypes, String returnType) throws InvalidArgumentException, LuaError, InvalidMixinException {
        noChef();
        LuaInjectorAnnotation type = new LuaCustom(state, annotation, annotationType.raw(), methodDescriptor, parameterTypes, returnType);
        chef = type;
        annotations.add(type);
        return this;
    }

    /// Create an @Expression annotation.
    /// Expressions are used in tandem with @Definition to make targeting where in the method to inject easier.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
    ///
    /// @see com.llamalad7.mixinextras.expression.Expression
    @LuaWrapped
    public MixinMethodBuilder expression(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        annotations.add(new LuaExpression(state, annotation));
        return this;
    }

    /// Create a @Definition annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
    ///
    /// @see com.llamalad7.mixinextras.expression.Definition
    @LuaWrapped
    public MixinMethodBuilder definition(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        annotations.add(new LuaDefinition(state, annotation));
        return this;
    }

    private void noChef() throws InvalidMixinException {
        if (chef != null) throw new InvalidMixinException(InvalidMixinException.Type.TOO_MANY_INJECTOR_ANNOTATIONS, classBuilder.visitedClass.name());
    }

    @LuaWrapped
    public MixinClassBuilder build() throws InvalidMixinException, InvalidArgumentException, LuaError {
        AbstractMixinBuilder.checkPhase();

        classBuilder.prepareMethod(chef, index, annotations, sugars);

        return classBuilder;
    }

}

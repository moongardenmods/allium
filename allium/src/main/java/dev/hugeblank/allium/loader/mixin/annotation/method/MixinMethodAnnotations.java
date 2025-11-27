package dev.hugeblank.allium.loader.mixin.annotation.method;

import dev.hugeblank.allium.loader.mixin.annotation.method.injectors.*;
import dev.hugeblank.allium.loader.type.annotation.LuaStateArg;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.List;

@LuaWrapped
public class MixinMethodAnnotations {

    /// Create a standard @Inject annotation.
    ///
    /// [Mixin Cheatsheet](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/inject.md)
    ///
    /// [Mixin Javadoc](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/Inject.html)
    ///
    /// @see org.spongepowered.asm.mixin.injection.Inject
    @LuaWrapped
    public static LuaInject inject(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaInject(state, annotation);
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
    public static LuaModifyArg modifyArg(@LuaStateArg LuaState state, LuaTable annotation, String targetType) throws InvalidArgumentException, LuaError {
        return new LuaModifyArg(state, annotation, targetType);
    }

    /// Create a @ModifyArgs annotation.
    ///
    /// [Mixin Cheatsheet](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/modify-args.md)
    ///
    /// [Mixin Javadoc](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArgs.html)
    ///
    /// @see org.spongepowered.asm.mixin.injection.ModifyArgs
    @LuaWrapped
    public static LuaModifyArgs modifyArgs(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaModifyArgs(state, annotation);
    }

    /// Create a @ModifyExpressionValue annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue)
    ///
    /// @param targetType a type descriptor string of the argument being modified.
    /// @see com.llamalad7.mixinextras.injector.ModifyExpressionValue
    @LuaWrapped
    public static LuaModifyExpressionValue modifyExpressionValue(@LuaStateArg LuaState state, LuaTable annotation, String targetType) throws InvalidArgumentException, LuaError {
        return new LuaModifyExpressionValue(state, annotation, targetType);
    }

    /// Create a @ModifyReturnValue annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyReturnValue)
    ///
    /// @see com.llamalad7.mixinextras.injector.ModifyReturnValue
    @LuaWrapped
    public static LuaModifyReturnValue modifyReturnValue(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaModifyReturnValue(state, annotation);
    }

    /// Create a @WrapMethod annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod)
    ///
    /// @see com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
    @LuaWrapped
    public static LuaWrapMethod wrapMethod(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaWrapMethod(state, annotation);
    }

    /// Supply a custom injector annotation that does not have an existing wrapper function.
    ///
    /// Method descriptor, parameter types and return type must be provided using java ASM syntax (ex. Lcom.example.package.ClassName;).
    ///
    /// Due to the inability to infer the expected parameters of many mixins, this function is offered as a catch-all.
    /// One must be incredibly explicit with your descriptor, parameter, and return types, or this will
    /// generate a method that will crash at the mixin apply phase. Here be dragons, you have been warned.
    @LuaWrapped
    public static LuaCustom custom(@LuaStateArg LuaState state, LuaTable annotation, EClass<?> annotationType, String methodDescriptor, List<String> parameterTypes, String returnType) throws InvalidArgumentException, LuaError {
        return new LuaCustom(state, annotation, annotationType.raw(), methodDescriptor, parameterTypes, returnType);
    }

    /// Create an @Expression annotation.
    /// Expressions are used in tandem with @Definition to make targeting where in the method to inject easier.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
    ///
    /// @see com.llamalad7.mixinextras.expression.Expression
    @LuaWrapped
    public static LuaExpression expression(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaExpression(state, annotation);
    }

    /// Create a @Definition annotation.
    ///
    /// [MixinExtras Wiki](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
    ///
    /// @see com.llamalad7.mixinextras.expression.Definition
    @LuaWrapped
    public static LuaDefinition definition(@LuaStateArg LuaState state, LuaTable annotation) throws InvalidArgumentException, LuaError {
        return new LuaDefinition(state, annotation);
    }

}

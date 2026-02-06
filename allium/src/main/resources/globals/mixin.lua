---@meta

---@class Mixin
mixin = {}

--- Mix into a specified class.
--- @param targetClass string The target class name, like what would be passed to `require()`.
--- @param interfaces table<string>? Optional interfaces to apply to the target class, using the `require()` format.
--- @param targetEnvironment string? The target environment, one of `"client"` or `"server"`. Leave `nil` to apply to common code.
--- @param duck true Make this a duck interface.
--- @return MixinInterfaceBuilder builder A mixin class builder.
function mixin.to(targetClass, interfaces, targetEnvironment, duck) end

--- Mix into a specified class.
--- @param targetClass string The target class name, like what would be passed to `require()`.
--- @param interfaces table<string>? Optional interfaces to apply to the target class, using the `require()` format.
--- @param targetEnvironment string? The target environment, one of `"client"` or `"server"`. Leave `nil` to apply to common code.
--- @param duck false? Do not make this a duck interface.
--- @return MixinClassBuilder builder A mixin class builder.
function mixin.to(targetClass, interfaces, targetEnvironment, duck) end

--- Get a reference to a mixin injection with the given `hookId`. This function can (and should) be used in `static` and `dynamic` script entrypoints.
--- @param hookId string The hook ID of the injector created in the `mixin` entrypoint.
function mixin.get(hookId) end

--- Provides the duck interface associated to a given ID. Used to access the accessors and invokers on objects belonging to the duck interface's target class.
--- @param mixinId string The ID of a duck interface mixin.
--- @return ClassUserdata interface The mixin interface.
function mixin.quack(mixinId) end

mixin.annotation = {}

--- Creates an [`@Inject`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/Inject.html) annotation.
---
--- For more information see:
--- - [Mixin Wiki - Advanced Mixin Usage - Callback Injectors](https://github.com/SpongePowered/Mixin/wiki/Advanced-Mixin-Usage--- Callback-Injectors)
--- - [Mixin Cheatsheet - `@Inject`](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/inject.md)
--- - [Mixin Cheatsheet - `@Inject`, cancellable](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/inject.md)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Inject`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/Inject.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.inject(annotation) end

--- Creates a [`@ModifyArg`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArg.html) annotation.
---
--- For more information see [Mixin Cheatsheet - `@ModifyArg`](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/modify-arg.md).
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@ModifyArg`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArg.html) annotation.
--- @param targetType string A type descriptor string of the argument being modified.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.

function mixin.annotation.modifyArg(annotation, targetType) end

--- Creates a [`@ModifyArgs`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArgs.html) annotation.
---
--- For more information see [Mixin Cheatsheet - `@ModifyArgs`](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/modify-args.md).
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@ModifyArgs`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/injection/ModifyArgs.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.modifyArgs(annotation) end

--- Creates a [`@ModifyExpressionValue`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/ModifyExpressionValue.html) annotation.
---
--- For more information see [MixinExtras - ModifyExpressionValue](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@ModifyExpressionValue`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/ModifyExpressionValue.html) annotation.
--- @param targetType string A type descriptor string of the argument being modified.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.modifyExpressionValue(annotation, targetType) end

--- Creates a [`@ModifyReturnValue`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/ModifyReturnValue.html) annotation.
---
--- For more information see [MixinExtras - ModifyReturnValue](https://github.com/LlamaLad7/MixinExtras/wiki/ModifyReturnValue)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@ModifyReturnValue`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/ModifyReturnValue.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.modifyReturnValue(annotation) end

--- Creates a [`@WrapMethod`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/wrapmethod/WrapMethod.html) annotation.
---
--- For more information see [MixinExtras - WrapMethod](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@WrapMethod`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/injector/wrapmethod/WrapMethod.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.wrapMethod(annotation) end

--- Creates a custom injector annotation.
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the  `annotationType`.
--- @param annotationType ClassUserdata The annotation class to be used.
--- @param methodDescriptor string The target method's descriptor string.
--- @param parameterTypes table<string> The parameters of the injector method. These may differ from the target method's descriptor.
--- @param returnType string The return type of the injector method. This may differ from the target method's descriptor.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.custom(annotation, annotationType, methodDescriptor, parameterTypes, returnType) end

--- Creates an [`@Expression`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/expression/Expression.html) annotation. This method does not produce an injector annotation and may be used multiple times within an inject method.
---
--- For more information see [MixinExtras - Expressions](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Expression`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/expression/Expression.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.expression(annotation) end

--- Creates a [`@Definition`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/expression/Definition.html) annotation. This method does not produce an injector annotation and may be used multiple times within an inject method.
---
--- For more information see [MixinExtras - Expressions](https://github.com/LlamaLad7/MixinExtras/wiki/Expressions)
---
--- @param annotation table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Definition`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/expression/Definition.html) annotation.
--- @return MixinAnnotationReference reference A reference to the annotation for use in mixin method building.
function mixin.annotation.definition(annotation) end

-- https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables

mixin.sugar = {}

--- Creates a parameter with a [`@Local`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/Local.html) annotation.
---
--- See [MixinExtras Wiki - Local](https://github.com/LlamaLad7/MixinExtras/wiki/Local) for more information.
---
--- Warning: Setting `mutable` to `true` changes the parameter's type from the one passed in `type` to one from the [`LocalRef` family](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/ref/package-summary.html).
---
--- @param type string A type descriptor string of the parameter.
--- @param annotation table? An optional [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Local`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/Local.html) annotation.
--- @param mutable boolean? Whether this parameter should be mutable.
--- @return MixinParameterReference reference A reference to the parameter and annotation for use in mixin method building.
function mixin.sugar.localref(type, annotation, mutable) end

--- Creates a parameter with a [`@Share`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/Share.html) annotation.
---
--- See [MixinExtras Wiki - Share](https://github.com/LlamaLad7/MixinExtras/wiki/Share) for more information.
---
--- @param type string A type descriptor string of the parameter.
--- @param annotation table? An optional [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Share`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/Share.html) annotation.
--- @return MixinParameterReference reference A reference to the parameter and annotation for use in mixin method building.
function mixin.sugar.share(type, annotation) end

--- Creates a parameter with a [`@Cancellable`](https://javadoc.io/doc/io.github.llamalad7/mixinextras-fabric/latest/com/llamalad7/mixinextras/sugar/Cancellable.html) annotation.
---
--- See [MixinExtras Wiki - Cancellable](https://github.com/LlamaLad7/MixinExtras/wiki/Cancellable) for more information.
---
--- @return MixinParameterReference reference A reference to the parameter and annotation for use in mixin method building.
function mixin.sugar.cancellable() end
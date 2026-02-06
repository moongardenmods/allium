--- @meta

---@class ClassUserdata A userdata representing a Java class.

---@class InstanceUserdata A userdata representing a Java object.

---@class ClassBuilder
local ClassBuilder = {}

--- Creates a field on the class.
--- @param fieldName string The name of the field being declared.
--- @param type ClassUserdata The type of the field being declared.
--- @param access table<string, boolean> The access values of the field being declared.
--- @param value boolean|number|string|nil An optional default value for the field to be set to.
function ClassBuilder:field(fieldName, type, access, value) end

--- Creates a ConstructorHandler that targets an existing constructor in the class being built.
--- @param parameters table<ClassUserdata> The list of parameters the targeted constructor uses.
--- @param remapper function? An optional function to handle reordering a constructors custom parameters to the ones expected by the targeted constructor.
--- @return ConstructorHandler handler The handler built from a matching constructor.
function ClassBuilder:usingThis(parameters, remapper) end

--- Creates a ConstructorHandler that targets a constructor in the parent class.
--- @param parameters table<ClassUserdata> The list of parameters the targeted super constructor uses.
--- @param remapper function An optional function to handle reordering a constructors custom parameters to the ones expected by the targeted super constructor.
--- @return ConstructorHandler handler The handler built from a matching super constructor.
function ClassBuilder:usingSuper(parameters, remapper) end

--- Creates or overrides a constructor.
--- @param handler ConstructorHandler A constructor handler that represents the constructor to be called within this one.
--- @param customParameters table<ClassUserdata>? An optional list of custom parameters that the handler is expected to remap using.
--- @param access table<string, boolean>? An optional set of access flags for the constructor.
--- @param definesFields boolean? An optional flag to indicate that any instance fields declared will be defined in this constructor.
function ClassBuilder:constructor(handler, customParameters, access, definesFields) end

--- Override an existing method of the parent class.
--- @param methodName string The name of the method to override.
--- @param parameters table<ClassUserdata> The parameters of the method to override.
function ClassBuilder:override(methodName, parameters) end

--- Create an entirely new method on the class.
--- @param methodName string The method name. Must not exist in any parent classes.
--- @param parameters table<ClassUserdata> A table representing the parameter types in order.
--- @param returnClass ClassUserdata The class to be returned by this method.
--- @param access table<string, boolean> Access flags for the overriding method.
function ClassBuilder:method(methodName, parameters, returnClass, access) end

--- Builds the class.
--- @return ClassUserdata class The completed class.
function ClassBuilder:build() end

---@class ConstructorHandler Provides context for creating a constructor that invokes another constructor within itself or its parent.

---@class MixinClassBuilder: MixinBuilder
local MixinClassBuilder = {}

--- Create an inject method. See [`mixin.to()`](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#mixin-to-targetclass-interfaces-targetenvironment-duck).
--- @param hookId string hook ID to later apply a hook onto this mixin with [`mixin.get()`](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#mixin-get-hookid).
--- @param methodAnnotations table<MixinAnnotationReference> Table of annotations to apply to the injector method. Requires exactly one injector annotation. See [Mixin Library - Annotations](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotations).
--- @param sugarParameters table<MixinParameterReference> Table of sugar parameters to apply after the last parameter of the injector method. See [Mixin Library - Sugars](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#sugars).
function MixinClassBuilder:createInjectMethod(hookId, methodAnnotations, sugarParameters) end

--- Builds the mixin.
function MixinClassBuilder:build() end

---@class MixinInterfaceBuilder
local MixinInterfaceBuilder = {}

--- Defines a setter and getter accessor using the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation. Can **only** be used on mixin builders where `duck` is `true`. See [`mixin.to()`](/reference/mixin-lib#mixin-to-targetclass-interfaces-targetenvironment-duck).
---
--- This is a convenience method that simply calls both `mixinInterfaceBuilder:getAccessor()` and `mixinInterfaceBuilder:setAccessor()` with the same `annotations` table.
---
--- For more information see [Mixin Cheatsheet - `@Accessor`](https://github.com/dblsaiko/mixin-cheatsheet/blob/master/accessor.md).
---
--- @param annotations table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation.
function MixinInterfaceBuilder:accessor(annotations) end

--- Defines a getter accessor using the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation. Can **only** be used on mixin builders where `duck` is `true`. See [`mixin.to()`](/reference/mixin-lib#mixin-to-targetclass-interfaces-targetenvironment-duck).
---
--- The method name is automatically generated from the target field name. It starts with `get`, then the first letter of the target field name is capitalized, and concatenated with the `get`. For example, given the target field `fooBar`, the method `getFooBar()` is created.
--- @param annotations table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation.
function MixinInterfaceBuilder:getAccessor(annotations) end

--- Defines a setter accessor using the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation. Can **only** be used on mixin builders where `duck` is `true`. See [`mixin.to()`](/reference/mixin-lib#mixin-to-targetclass-interfaces-targetenvironment-duck).

--- The method name is automatically generated from the target field name. It starts with `set`, then the first letter of the target field name is capitalized, and concatenated with the `set`. For example, given the target field `fooBar`, the method `setFooBar()` is created.
---
--- @param annotations table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Accessor`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Accessor.html) annotation.
function MixinInterfaceBuilder:setAccessor(annotations) end

--- Defines an invoker using the [`@Invoker`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Invoker.html) annotation. Can **only** be used on mixin builders where `duck` is `true`. See [`mixin.to()`](/reference/mixin-lib#mixin-to-targetclass-interfaces-targetenvironment-duck).
---
--- The method name is automatically generated from the target method name. It starts with `invoke`, then the first letter of the target method name is capitalized, and concatenated with the `invoke`. For example, given the target method `fooBar()`, the method `invokeFooBar()` is created.
---
--- @param annotations table An [annotation table](https://moongardenmods.github.io/allium-docs/reference/mixin-lib.html#annotation-tables) that matches the [`@Invoker`](https://jenkins.liteloader.com/view/Other/job/Mixin/javadoc/index.html?org/spongepowered/asm/mixin/gen/Invoker.html) annotation.
function MixinInterfaceBuilder:invoker(annotations) end

--- Builds the mixin.
--- @param mixinId string A unique ID representing the mixin class. Used to obtain the duck interface for accessing fields and invoking methods.
function MixinInterfaceBuilder:build(mixinId) end

---@class MixinAnnotationReference: InstanceUserdata
local MixinAnnotationReference = {}

---@class MixinParameterReference: InstanceUserdata
local MixinParameterReference = {}

---@class ResourceRegistration
local ResourceRegistration = {}

--- Closes the registered resource.
function ResourceRegistration:close() end
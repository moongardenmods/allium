---@meta

---@class Java
--- @field boolean userdata Representation of the primitive Java type: `boolean`.
--- @field byte userdata Representation of the primitive Java type: `byte`.
--- @field short userdata Representation of the primitive Java type: `short`.
--- @field int userdata Representation of the primitive Java type: `int`.
--- @field long userdata Representation of the primitive Java type: `long`.
--- @field float userdata Representation of the primitive Java type: `float`.
--- @field double userdata Representation of the primitive Java type: `double`.
--- @field char userdata Representation of the primitive Java type: `char`.
java = {}

--- Casts an object to the given class.
--- @param object InstanceUserdata An object instance.
--- @param class ClassUserdata A class that the object instance is cast to.
--- @return InstanceUserdata castObject The casted object.
function java.cast(object, class) end

--- Coerces a table-like Java object into a Lua table.
--- @param object InstanceUserdata A table-like object instance (one of `Map`, `List`, or `Set`).
--- @param class ClassUserdata A class representing both the type of `tableLike`, and the types of the elements within `tableLike`.
--- @return table castObject A table from the values contained within `tableLike`.
function java.coerce(tableLike, class) end

--- Wraps a Lua value in a provided class. Seldom useful due to type conversion being automatically handled by Allium.
--- @param value boolean|number|string|InstanceUserdata Lua value (or Java object) to be wrapped.
--- @param class ClassUserdata Class for value to be wrapped in.
--- @return ClassUserdata wrapped Wrapped value of type provided by `class`.
function java.wrap(value, class) end

--- Check whether the given object is of the given class.
--- @param object InstanceUserdata The object instance to test.
--- @param class ClassUserdata The class that the object instance is tested against.
--- @return ClassUserdata wrapped Whether or not `object` is of type `class`
function java.instanceOf(object, class) end

--- Invokes a java method (either static or instance) with the given `paramTypes`, using the given lua `params...`. Useful for when generic types abstract away the type, confusing Allium's automatic method resolution.
--- @param func function The method to be called.
--- @param class table<ClassUserdata> The list of classes representing each parameter.
--- @param params any... A variable amount of parameters to be passed to the method. If this method is an instance method, supply the instance that the method should be called on. Supplying the type of the instance in `paramTypes` is not necessary.
--- @return any... results Technically a variable number of arguments, but is generally just the single return value of the `function` being called.
function java.callWith(func, class, params) end

--- Gets a raw class or interface representation as an `EClass` (enhanced representation of the standard `Class` type). Seldom useful due to Allium's automatic conversion of `userdata [class]` to `EClass` and `Class` types where necessary.
--- @param className string The class name, as it would be provided to `require()`.
--- @return InstanceUserdata eClass An `EClass` instance representing the class matching `className`.
function java.getRawClass(className) end

--- Throw an exception in Java.
--- @param exception InstanceUserdata The exception instance to be thrown.
function java.exception(className) end

--- Provides a class builder extending from the given `superclass`. Optionally applies `interfaces`, and is created with the given `access`.
--- @param superclass ClassUserdata The parent class of the class being built.
--- @param interfaces? table<ClassUserdata> An optional table of interfaces to be applied to the class.
--- @param access? table Optional access flags for the class being built.
--- @return ClassBuilder builder A class builder.
function java.extendClass(superclass, interfaces, access) end
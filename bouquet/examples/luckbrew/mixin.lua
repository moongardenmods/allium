print("I'm feeling lucky!")

-- We have to be VERY careful about where we register our mixins. If your mixin is client-specific,
-- make sure to specify in mixin.to(), as well as checking that the package.environment is "client".

-- For registering our recipe in the right location
local BrewingRecipeRegistryMixinBuilder = mixin.to("net.minecraft.recipe.BrewingRecipeRegistry")

--[[ The following mess of a table is parsed into an annotation on the java side. We have to be very careful
    with how it's structured so that it doesn't blow up in our face. The important bits are that "method" is a table
    of strings, and at is a table of further "@At" annotations. Those have a special "value" that can just be the first
    index of a table if no other fields are necessary for further targetting. If you come from java modding this should
    make sense. If you come from the Lua side this is confusing. I recommend looking at the mixin cheatsheet for reference.
    https://github.com/2xsaiko/mixin-cheatsheet It should be possible to extrapolate from this, what an inject would look
    like in Lua.
--]]
addRecipes = BrewingRecipeRegistryMixinBuilder:inject("add_brewing_recipes", { -- Get the point at which potions should be registered.
    at = { { "TAIL" } },
    method = { "registerDefaults(Lnet/minecraft/recipe/BrewingRecipeRegistry$Builder;)V" }
})
-- Inject returns an event type for us to register to.
-- It is practice to register mixin events in another entrypoint, so we use it in dynamic!

BrewingRecipeRegistryMixinBuilder:build() -- Don't forget to actually build the mixin!
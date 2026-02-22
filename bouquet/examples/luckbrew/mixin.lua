print("I'm feeling lucky!")

-- We have to be VERY careful about where we register our mixins. If your mixin is client-specific,
-- make sure to specify in mixin.to(), as well as checking that allium.environment() is "client".

-- Furthermore, since the mixin entrypoint is invoked during the preLaunch phase, as a convention, absolutely no game
-- classes should be `require`d. For more information, see the [fabric wiki](https://wiki.fabricmc.net/documentation:entrypoint?s[]=prelaunch).
-- Note that the `mixin` script entrypoint corresponds to `preLaunch`, while `main` corresponds to `main`

-- For registering our recipe in the right location
mixin.to("net.minecraft.world.item.alchemy.PotionBrewing")
    -- To obtain the hook for this mixin later (during proper initialization), we give it a unique name.
    :method("addBrewingRecipes")
        -- This table defines an array of annotations to be applied to the mixin method. One of them MUST be an injector,
        -- which can be determined by looking at the return values of the functions in the `MixinMethodAnnotations` class.
        -- If the value returned inherits from `LuaInjectorAnnotation`, then it can be used.
        :inject({
            -- The first parameter is a required table with keys and values equivalent to the values expected from the
            -- respective annotation methods. For the @Inject annotation, `method` and `at` should be provided.
            at = { -- `at` is an array of @At annotations. Since it's an annotation, we pass another table.
                { -- Annotations have a property where if the method name is `value`, supplying a key is not necessary.
                    "TAIL" -- For that reason, we can just pass the required `value` string without explicitly defining it.
                }
            },
            method = { -- `method` is how the location to inject is defined. It is a java ASM name and descriptor string.
                -- Most sane java modding IDEs will provide you a way to copy this descriptor to your clipboard. Use them.
                "addVanillaMixes(Lnet/minecraft/world/item/alchemy/PotionBrewing$Builder;)V"
            }
        })
        :build()
    :build("potion_brewing_mixin") -- Don't forget to actually build the mixin!
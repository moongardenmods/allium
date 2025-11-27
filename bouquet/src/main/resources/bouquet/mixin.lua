local CommandsMixin = mixin.to("net.minecraft.commands.Commands")

CommandsMixin:createInjectMethod("commands_init", {
    mixin.annotation.inject({
        method = {"<init>(Lnet/minecraft/commands/Commands$CommandSelection;Lnet/minecraft/commands/CommandBuildContext;)V"},
        at = { {"TAIL"} }
    })
})

CommandsMixin:build()

local ArgumentTypeInfosMixin = mixin.to("net.minecraft.commands.synchronization.ArgumentTypeInfos")

ArgumentTypeInfosMixin:createInjectMethod("argument_type_infos_register", {
    mixin.annotation.inject({
        method = {"register(Lnet/minecraft/core/Registry;Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;)Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;"},
        at = { {"HEAD"} }
    })
})

ArgumentTypeInfosMixin:build()
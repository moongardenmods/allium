mixin.to("net.minecraft.commands.Commands")
    :method("initCommands")
        :inject({
            method = {"<init>(Lnet/minecraft/commands/Commands$CommandSelection;Lnet/minecraft/commands/CommandBuildContext;)V"},
            at = { {"TAIL"} }
        })
        :build()
    :build("commands_mixin")

mixin.to("net.minecraft.commands.synchronization.ArgumentTypeInfos")
    :method("registerArgumentTypeInfos")
        :inject({
            method = {"register(Lnet/minecraft/core/Registry;Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;)Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;"},
            at = { {"HEAD"} }
        })
        :build()
    :build("argument_type_infos_mixin")
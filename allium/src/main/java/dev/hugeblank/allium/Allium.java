/* ____       .---.      .---.     .-./`)    ___    _  ,---.    ,---.
 .'  __ `.    | ,_|      | ,_|     \ .-.') .'   |  | | |    \  /    |
/   '  \  \ ,-./  )    ,-./  )     / `-' \ |   .'  | | |  ,  \/  ,  |
|___|  /  | \  '_ '`)  \  '_ '`)    `-'`"` .'  '_  | | |  |\_   /|  |
   _.-`   |  > (*)  )   > (*)  )    .---.  '   ( \.-.| |  _( )_/ |  |
.'   _    | (  .  .-'  (  .  .-'    |   |  ' (`. _` /| | (_ o _) |  |
|  _( )_  |  `-'`-'|___ `-'`-'|___  |   |  | (_ (_) _) |  (_,_)  |  |
\ (_ o _) /   |        \ |        \ |   |   \ /  . \ / |  |      |  |
 '.(_,_).'    `--------` `--------` '---'    ``-'`-''  '--'      '-*/
// (c) hugeblank 2022
// See LICENSE for more information
package dev.hugeblank.allium;

import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final Path DUMP_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("allium-dump");
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ID).orElseThrow().getMetadata().getVersion().getFriendlyString();

    @Override
    public void onInitialize() {
        EnvType envType = FabricLoader.getInstance().getEnvironmentType();
        SetupHelpers.initializeExtensions(envType);
        SetupHelpers.initializeEnvironment();
    }
}

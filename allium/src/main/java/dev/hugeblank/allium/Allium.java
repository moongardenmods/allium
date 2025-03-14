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

import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.util.SetupHelpers;
import dev.hugeblank.allium.mappings.YarnLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Allium implements ModInitializer {

    public static final String ID = "allium";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final boolean DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final Path DUMP_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("allium-dump");
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ID).orElseThrow().getMetadata().getVersion().getFriendlyString();

    @Override
    public void onInitialize() {

        try {
            if (!Files.exists(FileHelper.PERSISTENCE_DIR)) Files.createDirectory(FileHelper.PERSISTENCE_DIR);
            if (!Files.exists(FileHelper.CONFIG_DIR)) Files.createDirectory(FileHelper.CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create config directory", e);
        }

        SetupHelpers.initializeEnvironment(EnvType.COMMON);
    }

    public enum EnvType {
        COMMON("common"), // common & server code
        CLIENT("client"), // client code only
        DEDICATED("dedicated"); // server code only

        private final String key;
        EnvType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

    }
}

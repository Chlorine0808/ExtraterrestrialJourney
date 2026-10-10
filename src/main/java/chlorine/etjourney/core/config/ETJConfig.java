package chlorine.etjourney.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Reads etjourney.cfg once during preInit. */
public final class ETJConfig {

    public static boolean debugLogging = false;
    /** Hidden dimensions biosphere terrain is generated in. */
    public static int sampleDimension = 77, netherSampleDimension = 78;

    private ETJConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        debugLogging = config.getBoolean(
            "debugLogging",
            Configuration.CATEGORY_GENERAL,
            false,
            "Log extra details for each lifecycle stage.");
        sampleDimension = config.getInt(
            "sampleDimension",
            "biospheres",
            77,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Hidden dimension that generates overworld terrain for biospheres. Change it if another mod uses 77.");
        netherSampleDimension = config.getInt(
            "netherSampleDimension",
            "biospheres",
            78,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Hidden dimension that generates Nether terrain for biospheres. Change it if another mod uses 78.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}

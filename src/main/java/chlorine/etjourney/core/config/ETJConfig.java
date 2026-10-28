package chlorine.etjourney.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Reads etjourney.cfg once during preInit. */
public final class ETJConfig {

    public static boolean debugLogging = false;
    /** Hidden dimensions biosphere terrain is generated in. */
    public static int sampleDimension = 77, netherSampleDimension = 78;
    /** Biomes (by ID or name) and mods (by mod ID) whose biomes are kept out of biospheres. */
    public static String[] excludedBiomes = new String[0], excludedMods = new String[0];
    /** Whether biosphere columns take their sample's biomes, for grass, leaf and water colours. */
    public static boolean biomeColors = true;

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
        biomeColors = config.getBoolean(
            "biomeColors",
            "biospheres",
            true,
            "Give the columns of each biosphere its sample's biomes, so grass, leaves and water take their colours."
                + " Land above or below a ball in the same columns takes them too.");
        excludedBiomes = config.getStringList(
            "excludedBiomes",
            "biospheres",
            new String[0],
            "Biomes never put in a biosphere, one per line: a biome ID, or a biome name (any case).");
        excludedMods = config.getStringList(
            "excludedMods",
            "biospheres",
            new String[0],
            "Mods whose biomes are never put in a biosphere, by mod ID; minecraft for the vanilla biomes.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}

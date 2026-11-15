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
    /** Client: whether End biomes tint the fog and the sky. */
    public static boolean biomeFog = true;
    /** Whether mod entities with a spawn rule are added to natural spawning. */
    public static boolean naturalSpawns = true;
    /** Whether enemies roll a rank when they join a world. */
    public static boolean enemyRanks = true;
    public static int[] rankDimensions = { 1 };
    /** Mobs that may roll a rank (empty: every monster) and mobs that never do, by EntityList name. */
    public static String[] rankedMobs = new String[0], unrankedMobs = new String[0];
    /** Potion IDs of ETJ's own status effects; a taken ID moves to the first free one. */
    public static int heavyPotionId = 100, frozenPotionId = 101;

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
        biomeFog = config.getBoolean(
            "biomeFog",
            "atmosphere",
            true,
            "Client only: tint the fog and the sky by End biome, and thicken the fog where the biome asks for it.");
        naturalSpawns = config.getBoolean(
            "naturalSpawns",
            "entities",
            true,
            "Let ETJ creatures spawn naturally in the outer End. Turn off to stop them without touching the world.");
        enemyRanks = config.getBoolean(
            "enemyRanks",
            "ranks",
            true,
            "Let enemies roll a rank (rare and above) when they join a world.");
        rankDimensions = config.get("ranks", "dimensions", new int[] { 1 }, "Dimensions whose enemies roll a rank.")
            .getIntList();
        rankedMobs = config.getStringList(
            "rankedMobs",
            "ranks",
            new String[0],
            "Mobs that may roll a rank, one EntityList name per line (as /summon takes them). Empty: every monster.");
        unrankedMobs = config.getStringList(
            "unrankedMobs",
            "ranks",
            new String[0],
            "Mobs that never roll a rank, by EntityList name; wins over rankedMobs. Bosses never roll one.");
        heavyPotionId = config.getInt(
            "heavyPotionId",
            "status",
            100,
            24,
            Integer.MAX_VALUE,
            "Potion ID of Heavy (lower jumps). A taken ID moves to the first free one; set it on server and client alike.");
        frozenPotionId = config.getInt(
            "frozenPotionId",
            "status",
            101,
            24,
            Integer.MAX_VALUE,
            "Potion ID of Frozen (no walking or jumping). A taken ID moves to the first free one.");
        if (config.hasChanged()) {
            config.save();
        }
    }
}

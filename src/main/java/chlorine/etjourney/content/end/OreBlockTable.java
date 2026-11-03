package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Registry names of the End ores and gems, the hardness and pickaxe level of each, the item it drops, the ore
 * dictionary names of both, and whether it survives explosions.
 */
final class OreBlockTable {

    static final class Entry {

        final String name;
        final EndBlock kind;
        final float hardness;
        final int harvestLevel;
        /** Registry name of the dropped item, and how many drop before Fortune. */
        final String drop;
        final int minDrops, maxDrops;
        final String oreName, dropOreName;
        /** Midgame and endgame ores survive explosions and the Wither, so only a pickaxe can mine them. */
        final boolean blastproof;

        Entry(String name, EndBlock kind, float hardness, int harvestLevel, String drop, int minDrops, int maxDrops,
            String oreName, String dropOreName, boolean blastproof) {
            this.name = name;
            this.kind = kind;
            this.hardness = hardness;
            this.harvestLevel = harvestLevel;
            this.drop = drop;
            this.minDrops = minDrops;
            this.maxDrops = maxDrops;
            this.oreName = oreName;
            this.dropOreName = dropOreName;
            this.blastproof = blastproof;
        }
    }

    // Vanilla pickaxe levels stand in until the mod has tools of its own; no vanilla pickaxe reaches the endgame's 4.
    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry(
                "ender_gold_ore",
                EndBlock.ENDER_GOLD_ORE,
                3.0F,
                1,
                "raw_ender_gold",
                1,
                1,
                "oreEnderGold",
                "rawEnderGold",
                false),
            new Entry(
                "ender_platinum_ore",
                EndBlock.ENDER_PLATINUM_ORE,
                3.0F,
                2,
                "raw_ender_platinum",
                1,
                1,
                "oreEnderPlatinum",
                "rawEnderPlatinum",
                false),
            new Entry(
                "sakura_gold_ore",
                EndBlock.SAKURA_GOLD_ORE,
                3.0F,
                2,
                "raw_sakura_gold",
                1,
                1,
                "oreSakuraGold",
                "rawSakuraGold",
                false),
            new Entry(
                "golden_palladium_ore",
                EndBlock.GOLDEN_PALLADIUM_ORE,
                3.0F,
                2,
                "raw_golden_palladium",
                1,
                1,
                "oreGoldenPalladium",
                "rawGoldenPalladium",
                false),
            new Entry("drift_ore", EndBlock.DRIFT_ORE, 3.0F, 2, "drift_shard", 1, 2, "oreDrift", "gemDrift", false),
            new Entry(
                "ascended_crystal_ore",
                EndBlock.ASCENDED_CRYSTAL,
                4.0F,
                2,
                "ascended_crystal",
                1,
                1,
                "oreAscendedCrystal",
                "gemAscendedCrystal",
                false),
            new Entry(
                "black_quartz_ore",
                EndBlock.BLACK_QUARTZ,
                4.0F,
                2,
                "black_quartz",
                1,
                1,
                "oreQuartzBlack",
                "gemQuartzBlack",
                false),
            new Entry(
                "rhenium_ore",
                EndBlock.RHENIUM_ORE,
                4.0F,
                2,
                "raw_rhenium",
                1,
                1,
                "oreRhenium",
                "rawRhenium",
                false),
            new Entry("osmium_ore", EndBlock.OSMIUM_ORE, 5.0F, 3, "raw_osmium", 1, 1, "oreOsmium", "rawOsmium", true),
            new Entry(
                "iridium_ore",
                EndBlock.IRIDIUM_ORE,
                5.0F,
                3,
                "raw_iridium",
                1,
                1,
                "oreIridium",
                "rawIridium",
                true),
            new Entry(
                "arceodymium_ore",
                EndBlock.ARCEODYMIUM_ORE,
                5.0F,
                3,
                "raw_arceodymium",
                1,
                1,
                "oreArceodymium",
                "rawArceodymium",
                true),
            new Entry(
                "ktangphor_ore",
                EndBlock.KTANGPHOR_ORE,
                5.0F,
                3,
                "raw_ktangphor",
                1,
                1,
                "oreKtangphor",
                "rawKtangphor",
                true),
            new Entry("coltan_ore", EndBlock.COLTAN_ORE, 5.0F, 3, "raw_coltan", 1, 1, "oreColtan", "rawColtan", true),
            new Entry(
                "americium_ore",
                EndBlock.AMERICIUM_ORE,
                8.0F,
                4,
                "raw_americium",
                1,
                1,
                "oreAmericium",
                "rawAmericium",
                true),
            new Entry(
                "actinium_ore",
                EndBlock.ACTINIUM_ORE,
                8.0F,
                4,
                "raw_actinium",
                1,
                1,
                "oreActinium",
                "rawActinium",
                true),
            new Entry(
                "neutron_ore",
                EndBlock.NEUTRON_ORE,
                8.0F,
                4,
                "raw_neutron",
                1,
                1,
                "oreNeutron",
                "rawNeutron",
                true)));

    private OreBlockTable() {}
}

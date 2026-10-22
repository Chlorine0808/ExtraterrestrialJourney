package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Registry names of the End ores and gems, with the hardness and pickaxe level each one asks for. */
final class OreBlockTable {

    static final class Entry {

        final String name;
        final EndBlock kind;
        final float hardness;
        final int harvestLevel;

        Entry(String name, EndBlock kind, float hardness, int harvestLevel) {
            this.name = name;
            this.kind = kind;
            this.hardness = hardness;
            this.harvestLevel = harvestLevel;
        }
    }

    // Vanilla pickaxe levels stand in until the mod has tools of its own.
    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry("ender_gold_ore", EndBlock.ENDER_GOLD_ORE, 3.0F, 1),
            new Entry("ender_platinum_ore", EndBlock.ENDER_PLATINUM_ORE, 3.0F, 2),
            new Entry("sakura_gold_ore", EndBlock.SAKURA_GOLD_ORE, 3.0F, 2),
            new Entry("golden_palladium_ore", EndBlock.GOLDEN_PALLADIUM_ORE, 3.0F, 2),
            new Entry("drift_ore", EndBlock.DRIFT_ORE, 3.0F, 2),
            new Entry("ascended_crystal_ore", EndBlock.ASCENDED_CRYSTAL, 4.0F, 2),
            new Entry("black_quartz_ore", EndBlock.BLACK_QUARTZ, 4.0F, 2)));

    private OreBlockTable() {}
}

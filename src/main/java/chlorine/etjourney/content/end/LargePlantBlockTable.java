package chlorine.etjourney.content.end;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Registry names of the large-plant and crystal blocks, how each behaves, and the light each gives. */
final class LargePlantBlockTable {

    enum Type {
        LOG,
        LEAVES,
        /** Leaves that shine. */
        GLOW_LEAVES,
        CAP,
        GLOW,
        STEM,
        CRYSTAL,
        BUDDING,
        BUD
    }

    static final class Entry {

        final String name;
        final EndBlock kind;
        final Type type;
        final int light;

        Entry(String name, EndBlock kind, Type type, int light) {
            this.name = name;
            this.kind = kind;
            this.type = type;
            this.light = light;
        }
    }

    /** Planks crafted from one trunk block, four to one. */
    static final class Wood {

        final String planks;
        final String log;

        Wood(String planks, String log) {
            this.planks = planks;
            this.log = log;
        }
    }

    /** A sapling that stands on one biome's surface, and the leaves that may drop it. */
    static final class Sapling {

        final String name;
        final EndBiome biome;
        final List<EndBlock> droppedBy;

        Sapling(String name, EndBiome biome, EndBlock... droppedBy) {
            this.name = name;
            this.biome = biome;
            this.droppedBy = Collections.unmodifiableList(Arrays.asList(droppedBy));
        }
    }

    static final String SHARD = "crystal_shard";

    static final List<Entry> ENTRIES = Collections.unmodifiableList(
        Arrays.asList(
            new Entry("grove_log", EndBlock.GROVE_LOG, Type.LOG, 0),
            new Entry("grove_leaves", EndBlock.GROVE_LEAVES, Type.LEAVES, 0),
            new Entry("grove_blossom", EndBlock.GROVE_BLOSSOM, Type.GLOW_LEAVES, 10),
            new Entry("highland_log", EndBlock.HIGHLAND_LOG, Type.LOG, 0),
            new Entry("highland_leaves", EndBlock.HIGHLAND_LEAVES, Type.LEAVES, 0),
            new Entry("mycelia_stem", EndBlock.MYCELIA_STEM, Type.LOG, 0),
            new Entry("mycelia_cap", EndBlock.MYCELIA_CAP, Type.CAP, 0),
            new Entry("mycelia_gill", EndBlock.MYCELIA_GILL, Type.CAP, 10),
            new Entry("amber_root", EndBlock.AMBER_ROOT, Type.LOG, 0),
            new Entry("amber_seed", EndBlock.AMBER_SEED, Type.GLOW, 10),
            new Entry("amber_stem", EndBlock.AMBER_STEM, Type.STEM, 0),
            new Entry("crystal", EndBlock.CRYSTAL, Type.CRYSTAL, 10),
            new Entry("budding_crystal", EndBlock.BUDDING_CRYSTAL, Type.BUDDING, 6),
            new Entry("crystal_bud_small", EndBlock.CRYSTAL_BUD_SMALL, Type.BUD, 1),
            new Entry("crystal_bud_medium", EndBlock.CRYSTAL_BUD_MEDIUM, Type.BUD, 2),
            new Entry("crystal_bud_large", EndBlock.CRYSTAL_BUD_LARGE, Type.BUD, 4),
            new Entry("crystal_cluster", EndBlock.CRYSTAL_CLUSTER, Type.BUD, 5)));

    static final List<Sapling> SAPLINGS = Collections.unmodifiableList(
        Arrays.asList(
            new Sapling("grove_sapling", EndBiome.GROVE, EndBlock.GROVE_LEAVES, EndBlock.GROVE_BLOSSOM),
            new Sapling("highland_sapling", EndBiome.HIGHLAND, EndBlock.HIGHLAND_LEAVES)));

    static final List<Wood> WOODS = Collections.unmodifiableList(
        Arrays.asList(
            new Wood("grove_planks", "grove_log"),
            new Wood("highland_planks", "highland_log"),
            new Wood("mycelia_planks", "mycelia_stem"),
            new Wood("amber_planks", "amber_root")));

    private LargePlantBlockTable() {}
}

package chlorine.etjourney.world.end;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.KindTable;

/** The real block and metadata for each EndBlock kind; content binds its own blocks over the vanilla stand-ins. */
public final class EndPalette {

    private static final KindTable<Block> BLOCKS = new KindTable<>();
    private static final KindTable<Integer> META = new KindTable<>();

    private EndPalette() {}

    /** Call during preInit only; generation reads the palette without locking. */
    public static void bind(EndBlock kind, Block block, int meta) {
        BLOCKS.bind(kind, block);
        META.bind(kind, meta);
    }

    /** Whether content bound a block of its own to the kind, rather than leaving the vanilla stand-in. */
    public static boolean isBound(EndBlock kind) {
        return BLOCKS.get(kind) != null;
    }

    /** The biome whose surface block this is, or null when it is no biome surface. */
    public static EndBiome biomeOfTop(Block block) {
        for (EndBiome biome : EndBiome.values()) {
            if (isBound(biome.top) && block(biome.top) == block) return biome;
        }
        return null;
    }

    public static Block block(EndBlock kind) {
        Block bound = BLOCKS.get(kind);
        return bound != null ? bound : standIn(kind);
    }

    public static int meta(EndBlock kind) {
        Integer bound = META.get(kind);
        return bound != null ? bound : standInMeta(kind);
    }

    private static Block standIn(EndBlock kind) {
        switch (kind) {
            case WATER:
                return Blocks.water;
            case SOLAR_TOP:
                return Blocks.netherrack;
            case SOLAR_FILL:
            case NEBULA_FILL:
                return Blocks.stained_hardened_clay;
            case VORTEX_TOP:
                return Blocks.packed_ice;
            case VORTEX_FILL:
                return Blocks.snow;
            case NEBULA_TOP:
                return Blocks.mycelium;
            case STARDUST_TOP:
                return Blocks.quartz_block;
            case STARDUST_FILL:
                return Blocks.sandstone;
            case CHAIN:
                return Blocks.obsidian;
            case GLASS:
                return Blocks.glass;
            default:
                return Blocks.end_stone;
        }
    }

    private static int standInMeta(EndBlock kind) {
        switch (kind) {
            case SOLAR_FILL:
                return 1;
            case NEBULA_FILL:
                return 10;
            default:
                return 0;
        }
    }
}

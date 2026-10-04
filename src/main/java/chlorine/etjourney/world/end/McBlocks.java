package chlorine.etjourney.world.end;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The real block and metadata for each EndBlock kind. */
final class McBlocks {

    private McBlocks() {}

    static Block block(EndBlock kind) {
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
            default:
                return Blocks.end_stone;
        }
    }

    static int meta(EndBlock kind) {
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

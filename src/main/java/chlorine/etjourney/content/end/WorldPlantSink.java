package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.feature.plant.Grower;
import chlorine.etjourney.world.end.feature.plant.PlantSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Writes a plant into a live world, only where the block is air or replaceable, with block updates (flag 3). */
final class WorldPlantSink implements PlantSink {

    private static final int MAX_HEADROOM = 64;

    private final World world;

    WorldPlantSink(World world) {
        this.world = world;
    }

    /**
     * Replaces the block at (x, y, z), a sapling or a small plant, with the grown plant. The old block is put back
     * when the plant does not fit or the chunks around are not loaded (nothing is loaded for it).
     */
    static boolean growInPlace(World world, Random random, int x, int y, int z, Grower grower) {
        int reach = grower.radius();
        if (!world.checkChunksExist(x - reach, y, z - reach, x + reach, y, z + reach)) return false;
        Block old = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockToAir(x, y, z);
        int headroom = 0;
        while (headroom < MAX_HEADROOM && y + headroom < 255 && world.isAirBlock(x, y + headroom, z)) headroom++;
        if (grower.grow(random, x, y, z, headroom, new WorldPlantSink(world))) return true;
        world.setBlock(x, y, z, old, meta, 3);
        return false;
    }

    @Override
    public void put(int x, int y, int z, EndBlock kind) {
        if (y < 0 || y > 255) return;
        Block at = world.getBlock(x, y, z);
        if (!world.isAirBlock(x, y, z) && !at.isReplaceable(world, x, y, z)) return;
        world.setBlock(x, y, z, EndPalette.block(kind), EndPalette.meta(kind), 3);
    }
}

package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.modifier.BlockSink;

/** Pieces the per-cell tree placements share. */
final class PlantCells {

    static final long K1 = 0x9E3779B97F4A7C15L, K2 = 0xC2B2AE3D27D4EB4FL;

    /** A shape's extent around its anchor. */
    interface Bounds {

        int minX();

        int maxX();

        int minY();

        int maxY();

        int minZ();

        int maxZ();
    }

    private PlantCells() {}

    /** The cell's randomness for the placement salted by {@code salt}. */
    static Random random(long seed, long salt, int cellX, int cellZ) {
        Random r = new Random(seed ^ salt ^ cellX * K1 ^ cellZ * K2);
        // Rehash: Random seeds that are close give nearly equal first draws.
        return new Random(r.nextLong() ^ r.nextLong());
    }

    /** Whether the shape's bounding box, anchored at (x, y, z), meets the sink's volume. */
    static boolean touches(int x, int y, int z, Bounds b, BlockSink s) {
        return x + b.maxX() >= s.originX() && x + b.minX() < s.originX() + s.width()
            && z + b.maxZ() >= s.originZ()
            && z + b.minZ() < s.originZ() + s.width()
            && y + b.maxY() >= s.minY()
            && y + b.minY() < s.maxY();
    }
}

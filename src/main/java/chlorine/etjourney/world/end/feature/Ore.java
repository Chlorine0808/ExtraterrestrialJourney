package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Hash;

/**
 * Ores and gems of the outer End: where in the rock each may lie, and how often its veins start. A vein is a short
 * random walk from a start drawn per 16-block cube, so both halves of a column and every chunk agree on it.
 */
public enum Ore {

    ENDER_GOLD(EndBlock.ENDER_GOLD_ORE, 6, 4, 8, 4, 24, 0, 255, null),
    ENDER_PLATINUM(EndBlock.ENDER_PLATINUM_ORE, 0.4, 2, 4, 16, Integer.MAX_VALUE, 0, 255, null),
    SAKURA_GOLD(EndBlock.SAKURA_GOLD_ORE, 2.5, 3, 6, 4, 24, 0, 255, EndBiome.GROVE),
    GOLDEN_PALLADIUM(EndBlock.GOLDEN_PALLADIUM_ORE, 2.5, 3, 6, 4, 24, 0, 255, EndBiome.HIGHLAND),
    DRIFT(EndBlock.DRIFT_ORE, 0.3, 1, 3, 1, Integer.MAX_VALUE, 150, 255, null),
    ASCENDED_CRYSTAL(EndBlock.ASCENDED_CRYSTAL, 0.8, 2, 5, 1, Integer.MAX_VALUE, 150, 255, null),
    BLACK_QUARTZ(EndBlock.BLACK_QUARTZ, 0.8, 2, 5, 1, Integer.MAX_VALUE, 0, 40, null),
    RHENIUM(EndBlock.RHENIUM_ORE, 0.12, 1, 2, 8, Integer.MAX_VALUE, 0, 255, null),
    // Midgame: Osmium keeps low and Iridium high, Ktangphor deep down, Coltan near the surface.
    OSMIUM(EndBlock.OSMIUM_ORE, 1.0, 3, 6, 8, Integer.MAX_VALUE, 0, 120, null),
    IRIDIUM(EndBlock.IRIDIUM_ORE, 1.0, 3, 6, 8, Integer.MAX_VALUE, 130, 255, null),
    ARCEODYMIUM(EndBlock.ARCEODYMIUM_ORE, 1.2, 3, 6, 4, Integer.MAX_VALUE, 0, 255, null),
    KTANGPHOR(EndBlock.KTANGPHOR_ORE, 0.8, 3, 5, 12, Integer.MAX_VALUE, 0, 80, null),
    COLTAN(EndBlock.COLTAN_ORE, 1.5, 3, 6, 4, 24, 0, 255, null),
    SEMISTABLE_AMERICIUM(EndBlock.SEMISTABLE_AMERICIUM_ORE, 1.0, 2, 4, 4, Integer.MAX_VALUE, 0, 255, null, true),
    SEMISTABLE_ACTINIUM(EndBlock.SEMISTABLE_ACTINIUM_ORE, 1.0, 2, 4, 4, Integer.MAX_VALUE, 0, 255, null, true),
    INACTIVE_NEUTRON(EndBlock.INACTIVE_NEUTRON_ORE, 0.4, 1, 3, 8, Integer.MAX_VALUE, 0, 255, null, true);

    public static final int CUBE = 16;
    /** The farthest a vein reaches from its start. */
    public static final int REACH = 8;

    public final EndBlock block;
    /** Expected vein starts per cube. */
    public final double perCube;
    public final int minSize, maxSize;
    /** Blocks below the surface of the rock, 0 being the surface block itself. */
    public final int minDepth, maxDepth;
    public final int minY, maxY;
    /** The only biome it lies in, off zone islands; null for anywhere. */
    public final EndBiome biome;
    /** Whether it lies only on zone islands, as the endgame ores do. */
    public final boolean onZones;

    Ore(EndBlock block, double perCube, int minSize, int maxSize, int minDepth, int maxDepth, int minY, int maxY,
        EndBiome biome) {
        this(block, perCube, minSize, maxSize, minDepth, maxDepth, minY, maxY, biome, false);
    }

    Ore(EndBlock block, double perCube, int minSize, int maxSize, int minDepth, int maxDepth, int minY, int maxY,
        EndBiome biome, boolean onZones) {
        this.block = block;
        this.perCube = perCube;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.minY = minY;
        this.maxY = maxY;
        this.biome = biome;
        this.onZones = onZones;
    }

    public interface VeinBlock {

        void at(int x, int y, int z);
    }

    /** Every block of this ore's veins that start in the cube (cx, cy, cz); some may lie outside the cube. */
    public void veins(long seed, int cx, int cy, int cz, VeinBlock out) {
        long s = seed ^ (0x7A3D1F6B92C4E058L + ordinal() * 0x9E3779B97F4A7C15L);
        int attempts = (int) Math.ceil(perCube);
        for (int i = 0; i < attempts; i++) {
            long si = s + i * 0x632BE59BD9B4E019L;
            if (Hash.hash01(si, cx, cy, cz) >= perCube / attempts) continue;
            int x = cx * CUBE + (int) (Hash.hash01(si + 1, cx, cy, cz) * CUBE);
            int y = cy * CUBE + (int) (Hash.hash01(si + 2, cx, cy, cz) * CUBE);
            int z = cz * CUBE + (int) (Hash.hash01(si + 3, cx, cy, cz) * CUBE);
            int size = minSize + (int) (Hash.hash01(si + 4, cx, cy, cz) * (maxSize - minSize + 1));
            for (int step = 0; step < size; step++) {
                out.at(x, y, z);
                double r = Hash.hash01(si + 5, x, y + step * 31, z);
                int d = r < 0.5 ? 1 : -1;
                if (r < 1 / 3.0 || r >= 5 / 6.0) x += d;
                else if (r < 2 / 3.0) z += d;
                else y += d;
            }
        }
    }
}

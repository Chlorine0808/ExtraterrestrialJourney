package chlorine.etjourney.world.end.feature;

import java.util.List;

/** Land height: continents and mountain feet in the warped frame, a ragged coast, and void bases removed. */
public final class Land {

    /** Land removed at full weight of a base without a continent. */
    private static final double VOID_DROP = 160;

    private Land() {}

    public static double height(long seed, List<Continent.Seed> seeds, List<Mountains.Mountain> mountains, double x,
        double z, double voidShare) {
        double[] w = Continent.warped(seed, x, z);
        double best = Math.max(Continent.rawHeight(seeds, w[0], w[1]), Mountains.footLand(mountains, w[0], w[1]));
        best += Continent.coast(seed, x, z) - voidShare * VOID_DROP;
        return Math.min(Continent.MAX_HEIGHT, best);
    }

    /** Convenience for one column: looks up seeds and mountains within range itself. */
    public static double height(long seed, double x, double z, double range, double voidShare) {
        return height(
            seed,
            Continent.seedsNear(seed, x, z, range + Continent.warpReach()),
            Mountains.near(seed, x, z, range),
            x,
            z,
            voidShare);
    }
}

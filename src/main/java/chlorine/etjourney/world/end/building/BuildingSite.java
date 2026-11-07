package chlorine.etjourney.world.end.building;

import chlorine.etjourney.world.end.feature.StructureProbe;

/** Where a standing building's bottom layer goes, measured at the corners and centre of its footprint. */
public final class BuildingSite {

    /** Ground lower than this is the void. */
    private static final double VOID = 0;
    private static final double WET_RANGE = 2;

    private BuildingSite() {}

    /**
     * The bottom layer's y: the highest of the five points less the sink, so the low side stands on the foundation.
     * Null where the points differ by more than maxSlope, or one is over the void or in lake water.
     */
    public static Integer groundY(StructureProbe standing, int minX, int minZ, int maxX, int maxZ, int maxSlope,
        int sink) {
        double[][] points = { { minX, minZ }, { maxX, minZ }, { minX, maxZ }, { maxX, maxZ },
            { (minX + maxX) / 2.0, (minZ + maxZ) / 2.0 } };
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (double[] p : points) {
            double g = standing.ground(p[0], p[1]);
            if (g < VOID || standing.wet(p[0], p[1], WET_RANGE)) return null;
            low = Math.min(low, g);
            high = Math.max(high, g);
        }
        if (high - low > maxSlope) return null;
        // floor(ground) is the top solid block: sinking by n puts the bottom layer in the top n blocks.
        return (int) Math.floor(high) + 1 - sink;
    }
}

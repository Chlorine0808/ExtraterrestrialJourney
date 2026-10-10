package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

/** Finds the floor height most columns of a cut-out share, so a ball is not filled from a pillar or a ledge. */
public final class FloorFinder {

    /** The blocks of the world a cut-out is taken from. */
    public interface Blocks {

        /** Something solid or liquid to stand on or in. */
        boolean ground(int x, int y, int z);

        boolean air(int x, int y, int z);
    }

    /** Columns are sampled this far apart, out to this share of the radius. */
    private static final int STEP = 4;
    private static final double REACH = 0.7;
    /** How far above or below a candidate a column's own floor may lie, and the air a floor needs over it. */
    private static final int SLACK = 4, HEADROOM = 6;

    private FloorFinder() {}

    /** The height from low to high whose floors the most columns share, or -1 where no column has a floor. */
    public static int broadest(Blocks blocks, int x, int z, double radius, int low, int high) {
        if (high < low) return -1;
        List<int[]> columns = new ArrayList<>();
        int reach = (int) (radius * REACH);
        for (int dx = -reach; dx <= reach; dx += STEP) {
            for (int dz = -reach; dz <= reach; dz += STEP) {
                if (dx * dx + dz * dz <= reach * reach) columns.add(new int[] { x + dx, z + dz });
            }
        }
        int base = low - SLACK, span = high - low + 1 + 2 * SLACK;
        boolean[][] floors = new boolean[columns.size()][span];
        for (int c = 0; c < columns.size(); c++) {
            int px = columns.get(c)[0], pz = columns.get(c)[1];
            for (int i = 0; i < span; i++) floors[c][i] = isFloor(blocks, px, base + i, pz);
        }
        // Ranked by columns with a floor near g, then by columns with a floor exactly at g, so a flat floor wins
        // over the heights just above it.
        int best = -1, bestNear = 0, bestExact = 0;
        for (int g = high; g >= low; g--) {
            int near = 0, exact = 0;
            for (boolean[] column : floors) {
                if (column[g - base]) exact++;
                for (int i = g - SLACK - base; i <= g + SLACK - base; i++) {
                    if (column[i]) {
                        near++;
                        break;
                    }
                }
            }
            if (near > bestNear || near == bestNear && exact > bestExact) {
                bestNear = near;
                bestExact = exact;
                best = g;
            }
        }
        return best;
    }

    private static boolean isFloor(Blocks blocks, int x, int y, int z) {
        if (!blocks.ground(x, y, z)) return false;
        for (int k = 1; k <= HEADROOM; k++) if (!blocks.air(x, y + k, z)) return false;
        return true;
    }
}

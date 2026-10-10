package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

/** Finds the height a cut-out is aligned to: the floor most columns share, or for caves the most open space. */
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

    /**
     * The floor height from low to high that puts the most air inside a ball of the radius whose centre is rise
     * above it, or -1 where there is none; caves are narrow, so a cave ball follows open space, not floors.
     * Among equally open heights the middle one is taken, which centres the ball on its chamber.
     */
    public static int roomiest(Blocks blocks, int x, int z, double radius, double rise, int low, int high) {
        if (high < low) return -1;
        int reach = (int) (radius * 0.9);
        int[] counts = new int[high - low + 1];
        int best = 0;
        for (int g = low; g <= high; g++) {
            int cy = (int) Math.round(g + rise), n = 0;
            for (int dx = -reach; dx <= reach; dx += STEP) {
                for (int dy = -reach; dy <= reach; dy += STEP) {
                    for (int dz = -reach; dz <= reach; dz += STEP) {
                        if (dx * dx + dy * dy + dz * dz <= reach * reach && blocks.air(x + dx, cy + dy, z + dz)) n++;
                    }
                }
            }
            counts[g - low] = n;
            best = Math.max(best, n);
        }
        if (best == 0) return -1;
        int first = -1, last = -1;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] != best) continue;
            if (first < 0) first = i;
            last = i;
        }
        return low + (first + last) / 2;
    }

    private static boolean isFloor(Blocks blocks, int x, int y, int z) {
        if (!blocks.ground(x, y, z)) return false;
        for (int k = 1; k <= HEADROOM; k++) if (!blocks.air(x, y + k, z)) return false;
        return true;
    }
}

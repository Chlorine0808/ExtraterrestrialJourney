package chlorine.etjourney.world.end.region;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.Warp;

/**
 * Warped Voronoi regions of about REGION blocks. Cells within BLEND of the nearest centre share the weight, so
 * borders blend smoothly.
 */
public final class RegionMap {

    public static final int REGION = 720;
    static final double BLEND = 96;
    private static final double WARP = 120, WARP_SCALE = 400;
    /** Cells searched around the point; 5x5 so no cell within BLEND of the nearest is ever missed. */
    private static final int SEARCH = 2;
    static final long SALT = 0x9B05688C2B3E6C1FL;

    private RegionMap() {}

    /** One region cell and its share of the weight at a point. */
    public static final class CellWeight {

        public final int cx, cz;
        public final double weight;

        CellWeight(int cx, int cz, double weight) {
            this.cx = cx;
            this.cz = cz;
            this.weight = weight;
        }
    }

    /** Centre of a cell in the warped frame. */
    public static double[] cellCentre(long seed, int cx, int cz) {
        long s = seed ^ SALT;
        return new double[] { (cx + 0.15 + 0.7 * Hash.hash01(s + 1, cx, cz)) * REGION,
            (cz + 0.15 + 0.7 * Hash.hash01(s + 2, cx, cz)) * REGION };
    }

    /** Cells sharing the weight at (x, z); the weights sum to 1. */
    public static List<CellWeight> nearCells(long seed, double x, double z) {
        double[] w = Warp.warp(seed ^ SALT, x, z, WARP, WARP_SCALE);
        int ox = (int) Math.floor(w[0] / REGION), oz = (int) Math.floor(w[1] / REGION);
        int size = (2 * SEARCH + 1) * (2 * SEARCH + 1);
        double[] distances = new double[size];
        int[] cxs = new int[size], czs = new int[size];
        double nearest = Double.MAX_VALUE;
        int n = 0;
        for (int i = -SEARCH; i <= SEARCH; i++) {
            for (int j = -SEARCH; j <= SEARCH; j++) {
                double[] c = cellCentre(seed, ox + i, oz + j);
                distances[n] = Math.hypot(w[0] - c[0], w[1] - c[1]);
                cxs[n] = ox + i;
                czs[n] = oz + j;
                nearest = Math.min(nearest, distances[n]);
                n++;
            }
        }
        List<CellWeight> out = new ArrayList<>();
        double total = 0;
        double[] weights = new double[n];
        for (int k = 0; k < n; k++) {
            double t = 1 - (distances[k] - nearest) / BLEND;
            if (t <= 0) continue;
            weights[k] = t * t;
            total += weights[k];
        }
        for (int k = 0; k < n; k++) {
            if (weights[k] > 0) out.add(new CellWeight(cxs[k], czs[k], weights[k] / total));
        }
        return out;
    }
}

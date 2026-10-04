package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Hash;

/** Single mountains: rare tall peaks with broad feet that spread land around them. */
public final class Mountains {

    public static final int CELL = 320;
    private static final double CHANCE = 0.55;
    private static final double MIN_PEAK = 18, MAX_PEAK = 168;
    public static final double MAX_RADIUS = radiusFor(MAX_PEAK);
    /** A mountain only rises where its centre already stands on land this high. */
    private static final double ANCHOR_HEIGHT = 20;
    /** Land gained per block inward from a mountain's foot, close to the continents' own slope. */
    private static final double FOOT_SLOPE = 2.4;
    private static final long SALT = 0x1F83D9ABFB41BD6BL;
    private static final CellCache<Mountain> CELLS = new CellCache<>(4096);

    private Mountains() {}

    public static final class Mountain {

        public final double x, z, peak, radius;

        Mountain(double x, double z, double peak) {
            this.x = x;
            this.z = z;
            this.peak = peak;
            this.radius = radiusFor(peak);
        }
    }

    /** Taller mountains are much wider, so their foothills spread far. */
    static double radiusFor(double peak) {
        return 64 + peak * 2.0;
    }

    public static List<Mountain> near(long seed, double x, double z, double range) {
        List<Mountain> out = new ArrayList<>();
        double reach = range + MAX_RADIUS + Continent.warpReach();
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Mountain m = inCell(seed, cx, cz);
                if (m != null) out.add(m);
            }
        }
        return out;
    }

    public static Mountain inCell(long seed, int cx, int cz) {
        return CELLS.get(seed, cx, cz, Mountains::compute);
    }

    private static Mountain compute(long seed, int cx, int cz) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        double mx = (cx + Hash.hash01(s + 1, cx, cz)) * CELL;
        double mz = (cz + Hash.hash01(s + 2, cx, cz)) * CELL;
        if (Continent.rawHeight(Continent.seedsNear(seed, mx, mz, 0), mx, mz) < ANCHOR_HEIGHT) return null;
        // Cubing a uniform roll makes low peaks common and tall ones rare.
        double u = Hash.hash01(s + 3, cx, cz);
        return new Mountain(mx, mz, MIN_PEAK + (MAX_PEAK - MIN_PEAK) * u * u * u);
    }

    /** Land a mountain spreads under its foothills, measured at warped coordinates (wx, wz). */
    public static double footLand(List<Mountain> mountains, double wx, double wz) {
        double best = -100;
        for (Mountain m : mountains) {
            best = Math.max(best, Math.min(100, (m.radius - Math.hypot(wx - m.x, wz - m.z)) * FOOT_SLOPE));
        }
        return best;
    }

    /** Height of the tallest mountain over (wx, wz), before ridges and interior fading. */
    public static double rise(List<Mountain> mountains, double wx, double wz) {
        double best = 0;
        for (Mountain m : mountains) {
            double t = Math.hypot(wx - m.x, wz - m.z) / m.radius;
            if (t < 1) best = Math.max(best, m.peak * Math.pow(1 - t, 1.6));
        }
        return best;
    }
}

package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.Warp;

/** Sinkholes through a continent into the void, opening through a warped, rippled funnel. */
public final class Holes {

    public static final int CELL = 104;
    private static final double CHANCE = 0.75;
    /** Base radius 5-15 (small ones common), then widened 1 to 3 times. */
    private static final double MIN_RADIUS = 5, BASE_RADIUS = 15, MAX_WIDEN = 3;
    private static final double MAX_RADIUS = BASE_RADIUS * MAX_WIDEN;
    private static final double MIN_LAND = 40;
    private static final double MAX_GROUND = 124;
    private static final double WARP = 14;
    /** The funnel's width wanders between half and this multiple of its base width. */
    private static final double MAX_STRETCH = 1.5;
    private static final long SALT = 0xBB67AE8584CAA73BL;
    private static final CellCache<Hole> CELLS = new CellCache<>(8192);

    private Holes() {}

    public interface Probe {

        boolean allowed(double x, double z);

        double land(double x, double z);

        /** Ground surface at (x, z). */
        double top(double x, double z);

        List<Lakes.Lake> lakesNear(double x, double z, double range);
    }

    public static final class Hole {

        public final double x, z, radius;

        Hole(double x, double z, double radius) {
            this.x = x;
            this.z = z;
            this.radius = radius;
        }
    }

    /** Outer edge of the funnel around a hole of this radius. */
    static double outerRadius(double radius) {
        return radius * 3 + 12;
    }

    public static List<Hole> near(long seed, double x, double z, double range, Probe probe) {
        List<Hole> out = new ArrayList<>();
        double reach = range + outerRadius(MAX_RADIUS) * MAX_STRETCH + WARP;
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Hole hole = inCell(seed, cx, cz, probe);
                if (hole != null) out.add(hole);
            }
        }
        return out;
    }

    public static Hole inCell(long seed, int cx, int cz, Probe probe) {
        return CELLS.get(seed, cx, cz, (s, i, j) -> compute(s, i, j, probe));
    }

    private static Hole compute(long seed, int cx, int cz, Probe probe) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        double x = (cx + Hash.hash01(s + 1, cx, cz)) * CELL;
        double z = (cz + Hash.hash01(s + 2, cx, cz)) * CELL;
        if (!probe.allowed(x, z) || probe.land(x, z) < MIN_LAND) return null;
        // Only holes whose centre lies on ground below Y 124; their funnels may still climb higher.
        if (probe.top(x, z) >= MAX_GROUND) return null;
        double u = Hash.hash01(s + 3, cx, cz);
        double widen = 1 + (MAX_WIDEN - 1) * Hash.hash01(s + 4, cx, cz);
        double radius = (MIN_RADIUS + (BASE_RADIUS - MIN_RADIUS) * u * u) * widen;
        double clear = outerRadius(radius) * MAX_STRETCH + 4;
        // Water must not pour into the void, so holes keep clear of lake shores.
        for (Lakes.Lake lake : probe.lakesNear(x, z, clear)) {
            if (Math.hypot(x - lake.x, z - lake.z) < lake.radius * 1.6 + Lakes.SHORE_BAND + clear) return null;
        }
        return new Hole(x, z, radius);
    }

    /**
     * Share of a column's rock to remove from the top: 1 inside the shaft, easing to 0 across the funnel. Distances
     * are warped, the rim wanders between 0.6 and 1.4 radii, the funnel's width drifts and its slope ripples.
     */
    public static double cutFraction(long seed, List<Hole> holes, double x, double z) {
        if (holes.isEmpty()) return 0;
        long s = seed ^ SALT;
        double[] w = Warp.warp(s, x, z, WARP, 40);
        double rimNoise = Fractal.fbm(s + 5, x, z, 12, 3);
        double stretch = 0.5 + (MAX_STRETCH - 0.5) * Fractal.fbm(s + 6, x, z, 30, 3);
        double ripple = (Fractal.fbm(s + 7, x, z, 9, 2) - 0.5) * 0.45;
        double best = 0;
        for (Hole hole : holes) {
            double d = Math.hypot(w[0] - hole.x, w[1] - hole.z);
            double rim = hole.radius * (0.6 + 0.8 * rimNoise);
            if (d < rim) return 1;
            double outer = rim + (outerRadius(hole.radius) - hole.radius) * stretch;
            if (d >= outer) continue;
            double t = Math.min(1, Math.max(0, (d - rim) / (outer - rim) + ripple));
            best = Math.max(best, 1 - t * t * (3 - 2 * t));
        }
        return best;
    }
}

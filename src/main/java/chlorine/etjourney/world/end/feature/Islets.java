package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Hash;

/** Small flat-topped islets, like the small End islands of 1.9+; too small for the density grid. */
public final class Islets {

    public static final int CELL = 28;
    private static final double CHANCE = 0.45;
    private static final double MIN_RADIUS = 3, MAX_RADIUS = 9;
    private static final double MIN_Y = 24, MAX_Y = 200;
    /** An islet only forms where its style holds at least this weight. */
    private static final double MIN_WEIGHT = 0.6;
    private static final long SALT = 0xA54FF53A5F1D36F1L;
    private static final CellCache<Islet> CELLS = new CellCache<>(16384);

    private Islets() {}

    public interface Probe {

        double weight(double x, double z);

        List<ZoneIslands.Island> zoneIslandsNear(double x, double z);
    }

    /** A flat top at y over a cone underside that narrows to nothing depth blocks lower. */
    public static final class Islet {

        public final double x, z, radius, y, flat, depth;

        Islet(double x, double z, double radius, double y, double flat, double depth) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.y = y;
            this.flat = flat;
            this.depth = depth;
        }
    }

    public static List<Islet> near(long seed, double x, double z, double range, Probe probe) {
        List<Islet> out = new ArrayList<>();
        double reach = range + MAX_RADIUS;
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Islet islet = CELLS.get(seed, cx, cz, (s, i, j) -> compute(s, i, j, probe));
                if (islet != null) out.add(islet);
            }
        }
        return out;
    }

    private static Islet compute(long seed, int cx, int cz, Probe probe) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        double x = (cx + Hash.hash01(s + 1, cx, cz)) * CELL;
        double z = (cz + Hash.hash01(s + 2, cx, cz)) * CELL;
        if (probe.weight(x, z) < MIN_WEIGHT) return null;
        double u = Hash.hash01(s + 3, cx, cz);
        double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * u * u;
        for (ZoneIslands.Island island : probe.zoneIslandsNear(x, z)) {
            if (Math.hypot(x - island.x, z - island.z) < island.radius * 1.3 + radius + 8) return null;
        }
        double y = MIN_Y + (MAX_Y - MIN_Y) * Hash.hash01(s + 4, cx, cz);
        double flat = 2 + 2 * Hash.hash01(s + 5, cx, cz);
        double depth = radius * (0.6 + 0.6 * Hash.hash01(s + 6, cx, cz));
        return new Islet(x, z, radius, y, flat, depth);
    }

    /**
     * Lowest and highest solid Y at horizontal distance d from an islet's centre, or null outside it. The flat top
     * keeps the full radius; below it the outline shrinks linearly to a point.
     */
    public static int[] span(Islet islet, double d) {
        if (d >= islet.radius) return null;
        double under = islet.flat + islet.depth * (1 - d / islet.radius);
        return new int[] { (int) Math.ceil(islet.y - under), (int) Math.floor(islet.y) };
    }
}

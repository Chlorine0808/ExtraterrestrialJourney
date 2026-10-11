package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Zone islands: separate floating islands, one zone each, placed only over open void and never touching a reserved
 * area. An island that would touch land or a reserved area is left out whole, never cut.
 */
public final class ZoneIslands {

    public static final int CELL = 192;
    private static final double CHANCE = 0.25;
    private static final double MIN_DISTANCE = 1100;
    private static final double MIN_RADIUS = 48, MAX_RADIUS = 96;
    /** Minimum void gap between two islands, so islands of different zones never merge. */
    private static final double GAP = 8;
    private static final double MIN_Y = 28, MAX_Y = 200;
    /** Islands avoid land within this multiple of their radius. */
    private static final double LAND_REACH = 1.3;
    /** The terrain grid's density over open void, the floor under every island node. */
    private static final double VOID_DENSITY = -20;
    private static final long SALT = 0x2545F4914F6CDD1DL;
    private static final long SHAPE_SALT = 0x6A09E667F3BCC908L;
    private static final CellCache<Island> CELLS = new CellCache<>(4096);

    private ZoneIslands() {}

    /** Continent land height at a column, supplied by the engine. */
    public interface LandProbe {

        double land(double x, double z);
    }

    /** Centre, radius, dome height above and depth below the centre, and the underside's superellipse exponent. */
    public static final class Island {

        public final double x, y, z, radius, up, down, bottomShape;

        Island(double x, double y, double z, double radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.up = radius * 0.18;
            this.down = radius * 0.7 * (0.5 + 0.3 * Hash.hash01(SHAPE_SALT + 1, (int) x, (int) z));
            this.bottomShape = 1 + 2 * Hash.hash01(SHAPE_SALT, (int) x, (int) z);
        }
    }

    public static List<Island> near(long seed, double x, double z, double range, LandProbe land) {
        List<Island> out = new ArrayList<>();
        int c0x = (int) Math.floor((x - range) / CELL), c1x = (int) Math.floor((x + range) / CELL);
        int c0z = (int) Math.floor((z - range) / CELL), c1z = (int) Math.floor((z + range) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Island island = inCell(seed, cx, cz, land);
                if (island != null) out.add(island);
            }
        }
        return out;
    }

    public static Island inCell(long seed, int cx, int cz, LandProbe land) {
        return CELLS.get(seed, cx, cz, (s, i, j) -> compute(s, i, j, land));
    }

    /** Drops whole any island whose footprint touches a reserved area or its fade ring. */
    public static List<Island> withoutReserved(List<Island> islands, List<Area> reserved) {
        if (reserved.isEmpty()) return islands;
        List<Island> kept = new ArrayList<>();
        for (Island island : islands) {
            // High enough, it floats over the reserved island instead.
            if (island.y - island.down >= Reservations.CLEAR_Y
                || !Reservations.overlaps(reserved, island.x, island.z, island.radius * 1.2)) kept.add(island);
        }
        return kept;
    }

    private static Island compute(long seed, int cx, int cz, LandProbe land) {
        Island raw = raw(seed, cx, cz);
        if (raw == null) return null;
        double radius = raw.radius;
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int j = cz - 1; j <= cz + 1; j++) {
                if (i == cx && j == cz) continue;
                Island other = raw(seed, i, j);
                if (other == null) continue;
                double half = Distance.planar(raw.x - other.x, raw.z - other.z) / 2 - GAP / 2;
                radius = Math.min(radius, half / 1.2);
            }
        }
        if (radius < 24) return null;
        Island island = radius == raw.radius ? raw : new Island(raw.x, raw.y, raw.z, radius);
        return touchesLand(island, land) ? null : island;
    }

    private static Island raw(long seed, int cx, int cz) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        double ix = cx * CELL + 48 + Hash.hash01(s + 1, cx, cz) * (CELL - 96);
        double iz = cz * CELL + 48 + Hash.hash01(s + 2, cx, cz) * (CELL - 96);
        if (Distance.planar(ix, iz) < MIN_DISTANCE) return null;
        double radius = MIN_RADIUS + Hash.hash01(s + 3, cx, cz) * (MAX_RADIUS - MIN_RADIUS);
        // The top surface (y + 0.18 r * 1.2 edge) must stay below the world's ceiling.
        double maxY = Math.min(MAX_Y, 250 - radius * 0.18 * 1.2);
        double iy = MIN_Y + Hash.hash01(s + 4, cx, cz) * (maxY - MIN_Y);
        return new Island(ix, iy, iz, radius);
    }

    private static boolean touchesLand(Island island, LandProbe land) {
        double reach = island.radius * LAND_REACH;
        for (int a = 0; a < 16; a++) {
            double angle = a * Math.PI / 8;
            for (double r = 0; r <= reach; r += reach / 3) {
                if (land.land(island.x + Math.cos(angle) * r, island.z + Math.sin(angle) * r) > -20) return true;
            }
        }
        return false;
    }

    /** Rim noise for island density at a column, 0.8 to 1.2. */
    public static double edge(long seed, double x, double z) {
        return 0.8 + 0.4 * ValueNoise.mask(seed ^ SALT, x, z, 24);
    }

    /** Density of the islands: a flattened dome on top and a superellipse underside below the centre. */
    public static double density(List<Island> islands, double x, double y, double z, double edge) {
        double best = Double.NEGATIVE_INFINITY;
        for (Island island : islands) {
            double horiz = Distance.planar(x - island.x, z - island.z);
            double dy = y - island.y;
            double profile;
            if (dy >= 0) {
                double t = dy / island.up;
                profile = t >= 1 ? -1 : Math.sqrt(1 - t * t);
            } else {
                double t = -dy / island.down;
                double a = island.bottomShape;
                profile = t >= 1 ? -1 : Math.pow(1 - Math.pow(t, a), 1 / a);
            }
            best = Math.max(best, island.radius * edge * profile - horiz);
        }
        return best;
    }

    /**
     * Density at block (x, y, z) as the terrain grid lays it: the islands' density at the 8 x 4 x 8 grid nodes around
     * the block, interpolated the way the generator places blocks. Nodes are floored at the void's density, as the
     * terrain grid takes the higher of the two.
     */
    public static double gridDensity(long seed, List<Island> islands, int x, int y, int z) {
        int x0 = Math.floorDiv(x, 8) * 8, y0 = Math.floorDiv(y, 4) * 4, z0 = Math.floorDiv(z, 8) * 8;
        double tx = (x - x0) / 8.0, ty = (y - y0) / 4.0, tz = (z - z0) / 8.0;
        double[] c = new double[4];
        for (int n = 0; n < 4; n++) {
            int nx = x0 + (n & 1) * 8, nz = z0 + (n >> 1) * 8;
            double edge = edge(seed, nx, nz);
            double low = Math.max(VOID_DENSITY, density(islands, nx, y0, nz, edge));
            double high = Math.max(VOID_DENSITY, density(islands, nx, y0 + 4, nz, edge));
            c[n] = low + (high - low) * ty;
        }
        double near = c[0] + (c[1] - c[0]) * tx, far = c[2] + (c[3] - c[2]) * tx;
        return near + (far - near) * tz;
    }

    /** The island whose footprint (in radii, up to reach) covers (x, z), nearest first; null over open void. */
    public static Island ownerAt(List<Island> islands, double x, double z, double reach) {
        Island owner = null;
        double best = reach;
        for (Island island : islands) {
            double ratio = Distance.planar(x - island.x, z - island.z) / island.radius;
            if (ratio < best) {
                best = ratio;
                owner = island;
            }
        }
        return owner;
    }

    public static Zone zoneOf(long seed, Island island) {
        return Zone.at(seed, island.x, island.z);
    }
}

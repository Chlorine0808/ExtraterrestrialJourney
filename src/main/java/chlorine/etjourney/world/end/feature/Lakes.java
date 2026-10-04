package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Shallow lakes on the continents: a basin below the water, a flat shore ring above it, then a blend back into the
 * land. Leaks are tolerated; the flat ring keeps most of them shut.
 */
public final class Lakes {

    public static final int CELL = 184;
    private static final double CHANCE = 0.8;
    private static final double MIN_RADIUS = 14, MAX_RADIUS = 40;
    private static final double MIN_DEPTH = 2, MAX_DEPTH = 14;
    /** Width of the flat ring held 2 blocks above the water; it spans several 8-block density cells. */
    public static final double SHORE_BAND = 20;
    private static final double SHORE_BLEND = 0.6;
    private static final double MIN_LAND = 50, RING_LAND = 25;
    /** Skipped where a mountain lifts the centre by more than this. */
    private static final double MAX_SLOPE_RISE = 4;
    private static final int MAX_WATER_LEVEL = 118;
    /** Rock kept under a lake bed. */
    public static final double MIN_FLOOR = 12;
    private static final long SALT = 0x3C6EF372FE94F82AL;
    private static final CellCache<Lake> CELLS = new CellCache<>(4096);

    private Lakes() {}

    /** What placement needs to know about the terrain, supplied by the engine. */
    public interface Probe {

        boolean allowed(double x, double z);

        double land(double x, double z);

        /** Ground height with mountains, and without them, before any lake. */
        double ground(double x, double z, boolean mountains);

        double bottom(double x, double z);
    }

    public static final class Lake {

        public final double x, z, radius, depth;
        public final int waterLevel;

        Lake(double x, double z, double radius, int waterLevel, double depth) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.waterLevel = waterLevel;
            this.depth = depth;
        }
    }

    public static double reach() {
        return MAX_RADIUS * (1 + SHORE_BLEND) + SHORE_BAND;
    }

    public static List<Lake> near(long seed, double x, double z, double range, Probe probe) {
        List<Lake> out = new ArrayList<>();
        double reach = range + reach();
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                Lake lake = inCell(seed, cx, cz, probe);
                if (lake != null) out.add(lake);
            }
        }
        return out;
    }

    public static Lake inCell(long seed, int cx, int cz, Probe probe) {
        return CELLS.get(seed, cx, cz, (s, i, j) -> compute(s, i, j, probe));
    }

    private static Lake compute(long seed, int cx, int cz, Probe probe) {
        long s = seed ^ SALT;
        if (Hash.hash01(s, cx, cz) > CHANCE) return null;
        // Centres stay in the middle of their cells, so the flat rings of neighbours never meet.
        double x = (cx + 0.35 + 0.3 * Hash.hash01(s + 1, cx, cz)) * CELL;
        double z = (cz + 0.35 + 0.3 * Hash.hash01(s + 2, cx, cz)) * CELL;
        if (!probe.allowed(x, z) || probe.land(x, z) < MIN_LAND) return null;
        double ground = probe.ground(x, z, true);
        if (ground - probe.ground(x, z, false) > MAX_SLOPE_RISE) return null;
        int waterLevel = (int) Math.floor(ground) - 1;
        if (waterLevel > MAX_WATER_LEVEL) return null;
        double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * Hash.hash01(s + 3, cx, cz);
        // The whole shore ring must stand on land, or the water would sit beside the void.
        for (int a = 0; a < 16; a++) {
            double angle = a * Math.PI / 8, ring = radius + SHORE_BAND + 8;
            if (probe.land(x + Math.cos(angle) * ring, z + Math.sin(angle) * ring) < RING_LAND) return null;
        }
        // Squaring keeps most lakes shallow; thin ground caps the depth so rock stays below the water.
        double u = Hash.hash01(s + 4, cx, cz);
        double depth = Math
            .min(MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * u * u, ground - probe.bottom(x, z) - MIN_FLOOR - 1);
        if (depth < MIN_DEPTH) return null;
        return new Lake(x, z, radius, waterLevel, depth);
    }

    /** Drops whole any lake touching a reserved area: our land fades there, so the lake would hang over the void. */
    public static List<Lake> withoutReserved(List<Lake> lakes, List<Area> reserved) {
        if (lakes.isEmpty() || reserved.isEmpty()) return lakes;
        List<Lake> kept = new ArrayList<>();
        for (Lake lake : lakes) {
            double reach = lake.radius * (1 + SHORE_BLEND) + SHORE_BAND;
            if (!Reservations.overlaps(reserved, lake.x, lake.z, reach)) kept.add(lake);
        }
        return kept;
    }

    /** Ground after lakes reshape it: an uneven basin below the water, a flat ring above it, then a blend. */
    public static double shape(List<Lake> lakes, double top, double x, double z) {
        for (Lake lake : lakes) {
            double d = Math.hypot(x - lake.x, z - lake.z);
            double flatEnd = lake.radius + SHORE_BAND, blendEnd = flatEnd + lake.radius * SHORE_BLEND;
            if (d >= blendEnd) continue;
            double shore = lake.waterLevel + 2;
            if (d < lake.radius) {
                double t = d / lake.radius;
                double bed = 0.75 + 0.5 * ValueNoise.mask((long) lake.x * 31 + (long) lake.z, x, z, 7);
                top = lake.waterLevel - lake.depth * (1 - t * t) * bed;
            } else if (d < flatEnd) {
                top = shore;
            } else {
                double w = (blendEnd - d) / (blendEnd - flatEnd);
                top += (shore - top) * ValueNoise.smooth(w);
            }
        }
        return top;
    }

    /** Outer edge of the flat ring; inside it, ground below the water level is flooded. */
    public static double floodRadius(Lake lake) {
        return lake.radius + SHORE_BAND;
    }

    public static Lake floodAt(List<Lake> lakes, double x, double z) {
        for (Lake lake : lakes) {
            if (Math.hypot(x - lake.x, z - lake.z) < floodRadius(lake)) return lake;
        }
        return null;
    }
}

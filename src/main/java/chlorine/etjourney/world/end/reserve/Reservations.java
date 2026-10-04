package chlorine.etjourney.world.end.reserve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Warp;

/** Registered reservation providers, a per-region cache of their areas, and how areas suppress our terrain. */
public final class Reservations {

    /** Distance over which our land fades out around an area. */
    public static final double FADE = 96;
    /** The outline's radius wanders by this share either way, so the cut is not a circle. */
    public static final double WOBBLE = 0.15;
    /** Chunk radius covering every feature that can reach a chunk plus an HEE island's span. */
    public static final int SEARCH_CHUNKS = 28;
    private static final int REGION_SHIFT = 3;

    private static final List<ReservationProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Map<Long, List<Area>> REGION_CACHE = new ConcurrentHashMap<>();
    private static volatile long cachedSeed;

    private Reservations() {}

    public static void register(ReservationProvider provider) {
        PROVIDERS.add(provider);
    }

    /** For tests: forget every provider and cached area. */
    public static void reset() {
        PROVIDERS.clear();
        REGION_CACHE.clear();
    }

    public static List<Area> near(Object generator, Object world, int chunkX, int chunkZ, int chunkRadius) {
        List<Area> out = new ArrayList<>();
        for (ReservationProvider provider : PROVIDERS) {
            out.addAll(provider.near(generator, world, chunkX, chunkZ, chunkRadius));
        }
        return out;
    }

    /**
     * Areas for the 8x8-chunk region holding the chunk, computed once per region. Every chunk of a region sees the
     * same list, which covers SEARCH_CHUNKS around any of them.
     */
    public static List<Area> forChunk(Object generator, Object world, long seed, int chunkX, int chunkZ) {
        if (PROVIDERS.isEmpty()) return Collections.emptyList();
        if (seed != cachedSeed) {
            REGION_CACHE.clear();
            cachedSeed = seed;
        }
        int rx = chunkX >> REGION_SHIFT, rz = chunkZ >> REGION_SHIFT;
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        List<Area> areas = REGION_CACHE.get(key);
        if (areas == null) {
            int half = 1 << (REGION_SHIFT - 1);
            areas = Collections.unmodifiableList(
                near(generator, world, (rx << REGION_SHIFT) + half, (rz << REGION_SHIFT) + half, SEARCH_CHUNKS + half));
            if (REGION_CACHE.size() > 4096) REGION_CACHE.clear();
            REGION_CACHE.put(key, areas);
        }
        return areas;
    }

    /** True when a footprint (centre, radius) touches an area or its fade ring. */
    public static boolean overlaps(List<Area> areas, double x, double z, double radius) {
        for (Area area : areas) {
            if (Math.hypot(x - area.x, z - area.z) < radius + area.radius * (1 + WOBBLE) + FADE) return true;
        }
        return false;
    }

    /** True inside an area's island footprint plus margin (an HEE island's square reaches about 1.15 radii). */
    public static boolean insideFootprint(List<Area> areas, double x, double z, double margin) {
        for (Area area : areas) {
            if (Math.hypot(x - area.x, z - area.z) < area.radius * 1.2 + margin) return true;
        }
        return false;
    }

    /**
     * How strongly areas suppress our terrain at (x, z): 1 inside, easing to 0 across FADE. The outline is warped and
     * its radius wanders, so the land thins away like a coast.
     */
    public static double suppression(List<Area> areas, double x, double z) {
        double best = 0;
        for (Area area : areas) {
            if (Math.hypot(x - area.x, z - area.z) >= area.radius * (1 + WOBBLE) + FADE + 30) continue;
            long s = Double.doubleToLongBits(area.x) * 31 + Double.doubleToLongBits(area.z);
            double[] w = Warp.warp(s, x, z, 30, 90);
            double d = Math.hypot(w[0] - area.x, w[1] - area.z);
            double radius = area.radius * (1 - WOBBLE + 2 * WOBBLE * Fractal.fbm(s + 7, x, z, 60, 3));
            if (d <= radius) return 1;
            if (d >= radius + FADE) continue;
            double t = (d - radius) / FADE;
            best = Math.max(best, 1 - t * t * (3 - 2 * t));
        }
        return best;
    }
}

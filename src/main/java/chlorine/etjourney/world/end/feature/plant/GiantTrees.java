package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** Decides where the giant trees stand, about one per grove continent, the same from every seed of the continent. */
public final class GiantTrees {

    /** Horizontal reach of the roots, at most. */
    public static final int FOOTPRINT = 26;
    /** From the ground to the top of the branches. */
    public static final int HEIGHT = 56;
    static final int MIN_MEMBERS = 12;
    static final double LEVEL_RADIUS = 16, LEVEL_RISE = 14;
    private static final int CANDIDATES = 32;
    private static final double SPACING = 80;
    private static final long COUNT_SALT = 0x2545F4914F6CDD1DL;
    private static final long ORDER_SALT = 0x6C8E9CF570932BD5L;
    private static final Map<Long, List<double[]>> CACHE = new ConcurrentHashMap<>();
    private static volatile long cachedSeed;
    private static volatile boolean seeded;

    private GiantTrees() {}

    /** Roots {x, groundY, z} of the trees of the continent that {@code member} belongs to; empty off a grove. */
    public static List<double[]> anchors(long seed, Continent.Seed member, StructureProbe probe) {
        if (!seeded || seed != cachedSeed) {
            CACHE.clear();
            cachedSeed = seed;
            seeded = true;
        }
        long id = ContinentIds.of(seed, member);
        List<double[]> cached = CACHE.get(id);
        if (cached != null) return cached;
        List<double[]> out = compute(seed, member, probe);
        CACHE.put(id, out);
        return out;
    }

    /** The trees without the cache, so a test can ask each member afresh. */
    static List<double[]> compute(long seed, Continent.Seed member, StructureProbe probe) {
        return Collections.unmodifiableList(choose(seed, ContinentIds.of(seed, member), member, probe));
    }

    private static List<double[]> choose(long seed, long id, Continent.Seed member, StructureProbe probe) {
        List<double[]> out = new ArrayList<>();
        long[] keys = ContinentIds.members(seed, member)
            .clone();
        if (keys.length < MIN_MEMBERS || EndBiome.ofContinentOf(seed, member) != EndBiome.GROVE) return out;
        int wanted = 1 + (int) (keys.length / 1500.0 + Hash.hash01(seed ^ COUNT_SALT, (int) (id >> 32), (int) id));
        Long[] order = new Long[keys.length];
        for (int i = 0; i < keys.length; i++) order[i] = keys[i];
        final long orderSeed = seed ^ ORDER_SALT;
        Arrays.sort(
            order,
            (a, b) -> Double.compare(
                Hash.hash01(orderSeed, ContinentIds.cellX(a), ContinentIds.cellZ(a)),
                Hash.hash01(orderSeed, ContinentIds.cellX(b), ContinentIds.cellZ(b))));
        for (int i = 0; i < Math.min(CANDIDATES, order.length) && out.size() < wanted; i++) {
            Continent.Seed s = Continent.seedAt(seed, ContinentIds.cellX(order[i]), ContinentIds.cellZ(order[i]));
            if (s == null || tooClose(out, s.x, s.z) || !insideGrove(seed, s.x, s.z)) continue;
            // The roots crawl over the ground out to FOOTPRINT; on a lake they would hang over the water.
            if (probe.wet(s.x, s.z, FOOTPRINT)) continue;
            double ground = probe.ground(s.x, s.z);
            if (ground <= -100 || ground + HEIGHT > 250 || !level(probe, s.x, s.z, ground)) continue;
            out.add(new double[] { s.x, ground, s.z });
        }
        return out;
    }

    /** Whether the ground around the trunk is land within LEVEL_RISE of the root's: no cliff above, no void below. */
    private static boolean level(StructureProbe probe, double x, double z, double ground) {
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4;
            double g = probe.ground(x + LEVEL_RADIUS * Math.cos(angle), z + LEVEL_RADIUS * Math.sin(angle));
            if (Math.abs(g - ground) > LEVEL_RISE) return false;
        }
        return true;
    }

    private static boolean tooClose(List<double[]> taken, double x, double z) {
        for (double[] t : taken) if (Math.hypot(t[0] - x, t[2] - z) <= SPACING) return true;
        return false;
    }

    private static boolean insideGrove(long seed, double x, double z) {
        double r = FOOTPRINT * 0.6;
        return EndBiome.at(seed, x, z) == EndBiome.GROVE && EndBiome.at(seed, x + r, z) == EndBiome.GROVE
            && EndBiome.at(seed, x - r, z) == EndBiome.GROVE
            && EndBiome.at(seed, x, z + r) == EndBiome.GROVE
            && EndBiome.at(seed, x, z - r) == EndBiome.GROVE;
    }
}

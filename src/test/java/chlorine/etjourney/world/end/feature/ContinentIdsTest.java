package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class ContinentIdsTest {

    private static final long SEED = 42L;

    /** Seeds around a few places in the outer End. */
    private static List<Continent.Seed> sample() {
        List<Continent.Seed> out = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4, d = 1600 + k * 700;
            out.addAll(Continent.seedsNear(SEED, Math.cos(angle) * d, Math.sin(angle) * d, 160));
        }
        return out;
    }

    private static boolean overlap(Continent.Seed a, Continent.Seed b) {
        return Math.hypot(a.x - b.x, a.z - b.z) <= Continent.rimRadius(a) + Continent.rimRadius(b);
    }

    @Test
    void seedsWhoseLandOverlapsShareAnId() {
        List<Continent.Seed> seeds = sample();
        int pairs = 0;
        for (Continent.Seed a : seeds) {
            for (Continent.Seed b : seeds) {
                if (a == b || !overlap(a, b)) continue;
                assertEquals(ContinentIds.of(SEED, a), ContinentIds.of(SEED, b));
                pairs++;
            }
        }
        assertTrue(pairs > 100, "only " + pairs + " overlapping pairs");
    }

    @Test
    void separateLandmassesGetSeparateIds() {
        Set<Long> ids = new HashSet<>();
        for (Continent.Seed s : sample()) ids.add(ContinentIds.of(SEED, s));
        assertTrue(ids.size() > 3, "only " + ids.size() + " landmasses");
    }

    /** The id must not depend on which member the search started from, or chunks would disagree. */
    @Test
    void anyMemberFindsTheSameId() {
        for (Continent.Seed s : sample()) {
            long fresh = ContinentIds.compute(SEED, s);
            assertEquals(fresh, ContinentIds.of(SEED, s));
        }
        Continent.Seed first = sample().get(0);
        for (Continent.Seed s : sample()) {
            if (overlap(first, s)) assertEquals(ContinentIds.compute(SEED, first), ContinentIds.compute(SEED, s));
        }
    }

    /** Every member must list the same seeds, and each listed seed must belong to the same continent. */
    @Test
    void membersAreTheSameFromEverySeedOfAContinent() {
        long seed = 77L;
        Map<Long, List<Continent.Seed>> byId = new TreeMap<>();
        for (Continent.Seed s : Continent.seedsNear(seed, 3000, 0, 400))
            byId.computeIfAbsent(ContinentIds.of(seed, s), k -> new ArrayList<>())
                .add(s);
        List<Continent.Seed> group = byId.values()
            .stream()
            .filter(g -> g.size() >= 2)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no continent with two seeds near (3000, 0)"));
        Continent.Seed s1 = group.get(0), s2 = group.get(1);
        assertTrue(key(s1) != key(s2));

        long[] a = ContinentIds.members(seed, s1), b = ContinentIds.members(seed, s2);
        assertArrayEquals(sorted(a), sorted(b));
        assertTrue(contains(a, key(s1)) && contains(a, key(s2)));
        for (long k : a) {
            assertEquals(
                ContinentIds.of(seed, s1),
                ContinentIds.of(seed, Continent.seedAt(seed, ContinentIds.cellX(k), ContinentIds.cellZ(k))));
        }
    }

    private static long key(Continent.Seed s) {
        long cx = Continent.cellOf(s.x), cz = Continent.cellOf(s.z);
        return (cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static long[] sorted(long[] a) {
        long[] c = a.clone();
        Arrays.sort(c);
        return c;
    }

    private static boolean contains(long[] a, long k) {
        for (long v : a) if (v == k) return true;
        return false;
    }
}

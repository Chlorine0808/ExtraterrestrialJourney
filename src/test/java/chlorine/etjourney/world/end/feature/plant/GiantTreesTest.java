package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.ContinentIds;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Roughly one giant tree per grove continent: most have one, few have two, none stand outside a grove. */
class GiantTreesTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final StructureProbe PROBE = SAMPLER.structureProbe();
    private static final double MIN_RANGE = 1500, MAX_RANGE = 6000;

    /** One member seed per continent id found by the scan. */
    private static List<Continent.Seed> continents() {
        List<Continent.Seed> out = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        int reach = (int) (MAX_RANGE / Continent.SEED_CELL);
        for (int cx = -reach; cx <= reach; cx++) {
            for (int cz = -reach; cz <= reach; cz++) {
                Continent.Seed s = Continent.seedAt(SEED, cx, cz);
                if (s == null) continue;
                double d = Math.hypot(s.x, s.z);
                if (d < MIN_RANGE || d > MAX_RANGE) continue;
                if (ids.add(ContinentIds.of(SEED, s))) out.add(s);
            }
        }
        return out;
    }

    private static boolean grove(Continent.Seed s) {
        return EndBiome.ofContinentOf(SEED, s) == EndBiome.GROVE;
    }

    private static List<Continent.Seed> groves() {
        List<Continent.Seed> out = new ArrayList<>();
        for (Continent.Seed s : continents()) {
            if (grove(s) && ContinentIds.members(SEED, s).length >= GiantTrees.MIN_MEMBERS) out.add(s);
        }
        return out;
    }

    @Test
    void mostGroveContinentsHaveOne() {
        List<Continent.Seed> groves = groves();
        int withOne = 0;
        for (Continent.Seed s : groves) if (!GiantTrees.anchors(SEED, s, PROBE)
            .isEmpty()) withOne++;
        System.out.println("giant trees: " + groves.size() + " grove continents, " + withOne + " with one or more");
        assertTrue(groves.size() >= 10, "only " + groves.size() + " grove continents");
        assertTrue(withOne >= 0.6 * groves.size(), withOne + " of " + groves.size());
    }

    @Test
    void twoOrMoreIsUncommon() {
        List<Continent.Seed> groves = groves();
        int two = 0;
        for (Continent.Seed s : groves) if (GiantTrees.anchors(SEED, s, PROBE)
            .size() >= 2) two++;
        System.out.println("giant trees: " + two + " of " + groves.size() + " with two or more");
        assertTrue(two <= 0.25 * groves.size(), two + " of " + groves.size());
    }

    @Test
    void onlyOnGrove() {
        int checked = 0;
        for (Continent.Seed s : continents()) {
            if (grove(s)) continue;
            assertTrue(
                GiantTrees.anchors(SEED, s, PROBE)
                    .isEmpty());
            checked++;
        }
        assertTrue(checked > 5, "only " + checked + " other continents");
    }

    @Test
    void anchorsSitWellInsideGrove() {
        double r = GiantTrees.FOOTPRINT * 0.6;
        int seen = 0;
        for (Continent.Seed s : groves()) {
            for (double[] a : GiantTrees.anchors(SEED, s, PROBE)) {
                double[][] points = { { 0, 0 }, { r, 0 }, { -r, 0 }, { 0, r }, { 0, -r } };
                for (double[] p : points) assertEquals(EndBiome.GROVE, EndBiome.at(SEED, a[0] + p[0], a[2] + p[1]));
                assertEquals(PROBE.ground(a[0], a[2]), a[1], 1e-9);
                seen++;
            }
        }
        assertTrue(seen > 0);
    }

    /** Around the trunk the ground is land and fairly level: no cliff above the root, no void below it. */
    @Test
    void anchorsStandOnLevelLand() {
        int seen = 0;
        for (Continent.Seed s : groves()) {
            for (double[] a : GiantTrees.anchors(SEED, s, PROBE)) {
                for (int k = 0; k < 8; k++) {
                    double angle = k * Math.PI / 4;
                    double x = a[0] + GiantTrees.LEVEL_RADIUS * Math.cos(angle);
                    double z = a[2] + GiantTrees.LEVEL_RADIUS * Math.sin(angle);
                    double g = PROBE.ground(x, z);
                    assertTrue(Math.abs(g - a[1]) <= GiantTrees.LEVEL_RISE, "ground " + g + " near a root at " + a[1]);
                }
                seen++;
            }
        }
        assertTrue(seen > 0);
    }

    @Test
    void sameFromEveryMember() {
        int checked = 0;
        for (Continent.Seed s : groves()) {
            List<double[]> expected = GiantTrees.compute(SEED, s, PROBE);
            long[] members = ContinentIds.members(SEED, s);
            for (int m = 0; m < members.length; m += Math.max(1, members.length / 25)) {
                long k = members[m];
                Continent.Seed other = Continent.seedAt(SEED, ContinentIds.cellX(k), ContinentIds.cellZ(k));
                List<double[]> got = GiantTrees.compute(SEED, other, PROBE);
                assertEquals(expected.size(), got.size());
                for (int i = 0; i < got.size(); i++) assertArrayEquals(expected.get(i), got.get(i), 0);
            }
            if (!expected.isEmpty()) checked++;
            if (checked >= 3) break;
        }
        assertTrue(checked > 0);
    }
}

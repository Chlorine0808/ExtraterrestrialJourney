package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Each ball takes its terrain from the surface, a cave or the Nether, around a point chosen without generating. */
class BiosphereSourceTest {

    private static final long SEED = 41L;
    private static final BiosphereSource.Allowed ANY = (x, z) -> true;

    private static List<Biosphere> balls(int n) {
        List<Biosphere> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(new Biosphere(1000 + 113 * i, -700 + 71 * i, 120, 32, i));
        return out;
    }

    private static int count(List<Biosphere> balls, BiosphereSource.Kind kind) {
        int n = 0;
        for (Biosphere b : balls) if (BiosphereSource.kind(SEED, b) == kind) n++;
        return n;
    }

    @Test
    void sharesOfTheThreeKinds() {
        List<Biosphere> balls = balls(4000);
        double nether = count(balls, BiosphereSource.Kind.NETHER) / 4000.0;
        double cave = count(balls, BiosphereSource.Kind.CAVE) / 4000.0;
        assertTrue(Math.abs(nether - 0.15) < 0.02, "nether " + nether);
        assertTrue(Math.abs(cave - 0.85 * 0.10) < 0.015, "cave " + cave);
    }

    @Test
    void thePointIsTheSameEachTime() {
        Biosphere b = balls(1).get(0);
        BiosphereSource.Water dry = (x, z) -> false;
        assertArrayEquals(
            BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, dry, ANY),
            BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, dry, ANY));
    }

    @Test
    void mostSurfaceBallsAvoidWaterButSomeKeepIt() {
        // Water wherever x is negative.
        BiosphereSource.Water water = (x, z) -> x < 0;
        int wet = 0, n = 0;
        for (Biosphere b : balls(2000)) {
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, water, ANY);
            n++;
            if (water.at(p[0], p[1])) wet++;
        }
        double share = wet / (double) n;
        // About 15% take their first point as it is, and half of those land on water.
        assertTrue(share > 0.04 && share < 0.12, "wet share " + share);
    }

    @Test
    void allWaterStillGivesAPoint() {
        int[] p = BiosphereSource.point(SEED, balls(1).get(0), BiosphereSource.Kind.SURFACE, (x, z) -> true, ANY);
        assertNotNull(p);
    }

    @Test
    void netherPointsStayAnEighthAsFar() {
        for (Biosphere b : balls(500)) {
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.NETHER, (x, z) -> false, ANY);
            assertTrue(Math.abs(p[0]) <= BiosphereSource.SPREAD / 8 && Math.abs(p[1]) <= BiosphereSource.SPREAD / 8);
        }
    }

    /** A ball whose own world cannot be generated is cut from the other one rather than left empty. */
    @Test
    void eachKindFallsBackToTheOtherWorld() {
        assertArrayEquals(
            new BiosphereSource.Kind[] { BiosphereSource.Kind.NETHER, BiosphereSource.Kind.SURFACE },
            BiosphereSource.order(BiosphereSource.Kind.NETHER));
        assertArrayEquals(
            new BiosphereSource.Kind[] { BiosphereSource.Kind.SURFACE, BiosphereSource.Kind.NETHER },
            BiosphereSource.order(BiosphereSource.Kind.SURFACE));
        assertArrayEquals(
            new BiosphereSource.Kind[] { BiosphereSource.Kind.CAVE, BiosphereSource.Kind.NETHER },
            BiosphereSource.order(BiosphereSource.Kind.CAVE));
    }

    /** A point whose biomes the config keeps out is skipped for the next candidate. */
    @Test
    void excludedPointsAreSkipped() {
        BiosphereSource.Allowed east = (x, z) -> x >= 0;
        for (Biosphere b : balls(300)) {
            for (BiosphereSource.Kind kind : BiosphereSource.Kind.values()) {
                int[] p = BiosphereSource.point(SEED, b, kind, (x, z) -> false, east);
                if (p != null) assertTrue(p[0] >= 0, kind + " " + p[0]);
            }
        }
    }

    @Test
    void anAllowedPointBeatsTheWaterCheck() {
        // Land only where it is excluded: the ball takes allowed water rather than excluded land.
        BiosphereSource.Allowed east = (x, z) -> x >= 0;
        BiosphereSource.Water eastIsWater = (x, z) -> x >= 0;
        for (Biosphere b : balls(300)) {
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, eastIsWater, east);
            if (p != null) assertTrue(p[0] >= 0);
        }
    }

    /** With everything kept out there is no point, and the ball is then left out rather than crashing. */
    @Test
    void everythingExcludedGivesNoPoint() {
        for (BiosphereSource.Kind kind : BiosphereSource.Kind.values()) {
            assertNull(BiosphereSource.point(SEED, balls(1).get(0), kind, (x, z) -> false, (x, z) -> false));
        }
    }

    @Test
    void anExcludedFirstPointMovesToAnother() {
        for (Biosphere b : balls(200)) {
            int[] first = BiosphereSource.point(SEED, b, BiosphereSource.Kind.CAVE, (x, z) -> false, ANY);
            BiosphereSource.Allowed notFirst = (x, z) -> x != first[0] || z != first[1];
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.CAVE, (x, z) -> false, notFirst);
            assertNotNull(p);
            assertFalse(p[0] == first[0] && p[1] == first[1]);
        }
    }

    /** A 5 x 5 area of biome IDs, row by row with x fastest, all 1 but for one column set to 9. */
    private static int[] areaWith(int x, int z) {
        int[] ids = new int[25];
        Arrays.fill(ids, 1);
        ids[z * 5 + x] = 9;
        return ids;
    }

    @Test
    void anExcludedColumnInsideTheBallRulesTheFootprintOut() {
        assertFalse(BiosphereSource.clear(areaWith(2, 2), 5, 2, id -> id == 9));
        assertFalse(BiosphereSource.clear(areaWith(4, 2), 5, 2, id -> id == 9));
        assertTrue(BiosphereSource.clear(areaWith(2, 2), 5, 2, id -> id == 8));
    }

    @Test
    void columnsOutsideTheBallDoNotCount() {
        // The corner lies 2.8 blocks from the centre, beyond the radius of 2.
        assertTrue(BiosphereSource.clear(areaWith(0, 0), 5, 2, id -> id == 9));
    }
}

package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Each ball takes its terrain from the surface, a cave or the Nether, around a point chosen without generating. */
class BiosphereSourceTest {

    private static final long SEED = 41L;

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
            BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, dry),
            BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, dry));
    }

    @Test
    void mostSurfaceBallsAvoidWaterButSomeKeepIt() {
        // Water wherever x is negative.
        BiosphereSource.Water water = (x, z) -> x < 0;
        int wet = 0, n = 0;
        for (Biosphere b : balls(2000)) {
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.SURFACE, water);
            n++;
            if (water.at(p[0], p[1])) wet++;
        }
        double share = wet / (double) n;
        // About 15% take their first point as it is, and half of those land on water.
        assertTrue(share > 0.04 && share < 0.12, "wet share " + share);
    }

    @Test
    void allWaterStillGivesAPoint() {
        int[] p = BiosphereSource.point(SEED, balls(1).get(0), BiosphereSource.Kind.SURFACE, (x, z) -> true);
        assertNotNull(p);
    }

    @Test
    void netherPointsStayAnEighthAsFar() {
        for (Biosphere b : balls(500)) {
            int[] p = BiosphereSource.point(SEED, b, BiosphereSource.Kind.NETHER, (x, z) -> false);
            assertTrue(Math.abs(p[0]) <= BiosphereSource.SPREAD / 8 && Math.abs(p[1]) <= BiosphereSource.SPREAD / 8);
        }
    }
}

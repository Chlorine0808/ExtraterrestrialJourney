package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A biosphere is a glass ball about 32 blocks in radius, empty inside for the populate pass to fill. */
class BiosphereTest {

    /** Cells are cached per seed, so each probe gets a seed of its own. */
    private static final long SEED = 31L, EMPTY_SEED = 32L, VOID_SEED = 33L, DENSE_SEED = 34L;

    private static StructureProbe probe(double weight, double ground) {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return weight;
            }

            @Override
            public double land(double x, double z) {
                return ground > -100 ? 60 : 0;
            }

            @Override
            public double ground(double x, double z) {
                return ground;
            }
        };
    }

    private static Biosphere first(StructureProbe probe) {
        return first(SEED, probe);
    }

    private static Biosphere first(long seed, StructureProbe probe) {
        for (int cx = 20; cx < 60; cx++) {
            for (int cz = 20; cz < 60; cz++) {
                Biosphere b = Biosphere.KIND.inCell(seed, cx, cz, probe);
                if (b != null) return b;
            }
        }
        return null;
    }

    @Test
    void noneFormWithoutTheStyle() {
        assertNull(first(EMPTY_SEED, probe(0, 70)));
    }

    @Test
    void radiusIsAbout32AndTheBallStaysInTheWorld() {
        int n = 0;
        StructureProbe probe = probe(1, 70);
        for (int cx = 20; cx < 60; cx++) {
            for (int cz = 20; cz < 60; cz++) {
                Biosphere b = Biosphere.KIND.inCell(SEED, cx, cz, probe);
                if (b == null) continue;
                n++;
                assertTrue(b.radius >= 28 && b.radius <= 36, "radius " + b.radius);
                assertTrue(b.y + b.radius <= 248, "above the ceiling: " + b.y);
                assertTrue(b.y - b.radius >= 70 + 8, "into the ground: " + b.y);
            }
        }
        assertTrue(n > 20, "only " + n + " biospheres");
    }

    @Test
    void overTheVoidTheyStillFloatAboveTheBottom() {
        Biosphere b = first(VOID_SEED, probe(1, -1000));
        assertNotNull(b);
        assertTrue(b.y - b.radius >= 30, "too low: " + b.y);
    }

    @Test
    void glassShellAroundAnEmptyInside() {
        Biosphere b = first(probe(1, 70));
        assertNotNull(b);
        int x = (int) Math.floor(b.centreX), y = (int) Math.floor(b.y), z = (int) Math.floor(b.centreZ);
        assertEquals(Biosphere.Part.INSIDE, b.part(x, y, z));
        int top = y;
        while (b.part(x, top, z) == Biosphere.Part.INSIDE) top++;
        assertEquals(Biosphere.Part.GLASS, b.part(x, top, z));
        assertEquals(Biosphere.Part.OUTSIDE, b.part(x, top + 1, z));
    }

    @Test
    void theFloorLevelSitsInTheLowerHalf() {
        Biosphere b = first(probe(1, 70));
        assertNotNull(b);
        int x = (int) Math.floor(b.centreX), z = (int) Math.floor(b.centreZ);
        int floor = b.floorTop(x, z);
        assertTrue(floor < b.y && floor > b.y - b.radius * 0.5, "floor " + floor + " in ball at " + b.y);
    }

    @Test
    void theShellIsClosedAlongEveryAxis() {
        Biosphere b = first(probe(1, 70));
        assertNotNull(b);
        int cx = (int) Math.floor(b.centreX), cy = (int) Math.floor(b.y), cz = (int) Math.floor(b.centreZ);
        int[][] dirs = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] d : dirs) {
            boolean glass = false;
            for (int k = 0; k < 40 && !glass; k++) {
                glass = b.part(cx + d[0] * k, cy + d[1] * k, cz + d[2] * k) == Biosphere.Part.GLASS;
            }
            assertTrue(glass, "no glass along " + d[0] + "," + d[1] + "," + d[2]);
        }
    }

    @Test
    void aboutOnePerTwelveThousandSquareBlocksAndNeverTouching() {
        StructureProbe probe = probe(1, -1000);
        List<Biosphere> balls = new ArrayList<>();
        for (int cx = 20; cx < 60; cx++) {
            for (int cz = 20; cz < 60; cz++) {
                Biosphere b = Biosphere.KIND.inCell(DENSE_SEED, cx, cz, probe);
                if (b != null) balls.add(b);
            }
        }
        double area = Math.pow(40 * Biosphere.KIND.cell, 2);
        // Twice as many as one per 160 x 160 cell at a chance of a half.
        double perBall = area / balls.size();
        assertTrue(perBall < 160 * 160 / 0.5 / 1.8, "one per " + (int) perBall + " square blocks");
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                Biosphere a = balls.get(i), b = balls.get(j);
                double d = Math.hypot(a.centreX - b.centreX, a.centreZ - b.centreZ);
                assertTrue(d >= a.radius + b.radius + 2, "balls " + (int) d + " apart");
            }
        }
    }
}

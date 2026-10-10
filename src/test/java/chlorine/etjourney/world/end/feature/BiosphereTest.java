package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A biosphere is a glass ball about 32 blocks in radius holding a floor of one biome's blocks. */
class BiosphereTest {

    /** Cells are cached per seed, so each probe gets a seed of its own. */
    private static final long SEED = 31L, EMPTY_SEED = 32L, VOID_SEED = 33L;

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
                assertTrue(b.pick >= 0 && b.pick < 1, "pick " + b.pick);
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
    void glassShellAroundAFloorOfTopFillerAndDeepBlocks() {
        Biosphere b = first(probe(1, 70));
        assertNotNull(b);
        int x = (int) Math.floor(b.centreX), z = (int) Math.floor(b.centreZ);
        int top = b.floorTop(x, z);
        assertEquals(Biosphere.Part.TOP, b.part(x, top, z));
        assertEquals(Biosphere.Part.FILLER, b.part(x, top - 1, z));
        assertEquals(Biosphere.Part.FILLER, b.part(x, top - 3, z));
        assertEquals(Biosphere.Part.DEEP, b.part(x, top - 4, z));
        assertEquals(Biosphere.Part.AIR, b.part(x, top + 1, z));
        // Straight up from the floor the first solid block is the glass ceiling, then nothing.
        int y = top + 1;
        while (b.part(x, y, z) == Biosphere.Part.AIR) y++;
        assertEquals(Biosphere.Part.GLASS, b.part(x, y, z));
        assertEquals(Biosphere.Part.OUTSIDE, b.part(x, y + 1, z));
        // The floor sits in the lower half, leaving room for trees.
        assertTrue(y - top > b.radius, "headroom " + (y - top));
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
    void floorColumnsKeepClearOfTheGlass() {
        Biosphere b = first(probe(1, 70));
        assertNotNull(b);
        int x = (int) Math.floor(b.centreX), z = (int) Math.floor(b.centreZ);
        assertTrue(b.onFloor(x, z));
        assertTrue(!b.onFloor(x + (int) b.radius, z), "the rim column counts as floor");
    }
}

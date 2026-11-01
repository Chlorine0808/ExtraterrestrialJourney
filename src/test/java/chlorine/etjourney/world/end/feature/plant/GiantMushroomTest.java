package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The MYCELIA giant mushrooms: three sizes, each one piece, plain on top with gills lining the underside. */
class GiantMushroomTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void staysWithinEight() {
        for (GiantMushroom size : GiantMushroom.values()) {
            assertTrue(size.radius() <= 8);
            for (int seed = 0; seed < 100; seed++) {
                Voxels v = new Voxels();
                assertTrue(size.grow(new Random(seed), X, Y, Z, 40, v), size + " seed " + seed);
                assertTrue(v.maxDistance(X, Z) <= size.radius(), size + " seed " + seed);
                assertTrue(v.height(Y) <= 30, size + " seed " + seed);
                assertTrue(v.minY >= Y, size + " seed " + seed);
            }
        }
    }

    private static boolean isCap(EndBlock b) {
        return b == EndBlock.MYCELIA_CAP || b == EndBlock.MYCELIA_GILL;
    }

    @Test
    void capLooksPlainFromAbove() {
        for (GiantMushroom size : GiantMushroom.values()) {
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                size.grow(new Random(seed), X, Y, Z, 40, v);
                int columns = 0;
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        EndBlock highest = null;
                        for (int dy = 0; dy <= 31; dy++) {
                            EndBlock b = v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz));
                            if (b != null) highest = b;
                        }
                        if (!isCap(highest)) continue;
                        columns++;
                        assertEquals(EndBlock.MYCELIA_CAP, highest, size + " seed " + seed);
                    }
                }
                assertTrue(columns > 0);
                assertTrue(v.count(EndBlock.MYCELIA_STEM) > 0);
            }
        }
    }

    @Test
    void gillsLineTheUnderside() {
        for (GiantMushroom size : GiantMushroom.values()) {
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                size.grow(new Random(seed), X, Y, Z, 40, v);
                assertTrue(v.count(EndBlock.MYCELIA_GILL) > 0, size + " seed " + seed);
                for (Map.Entry<Long, EndBlock> e : v.cells.entrySet()) {
                    long k = e.getKey();
                    int x = Voxels.x(k), y = Voxels.y(k), z = Voxels.z(k);
                    boolean airBelow = !v.cells.containsKey(Voxels.key(x, y - 1, z));
                    boolean capAbove = isCap(v.cells.get(Voxels.key(x, y + 1, z)));
                    if (e.getValue() == EndBlock.MYCELIA_GILL) {
                        assertTrue(airBelow && capAbove, size + " seed " + seed + " gill at " + x + "," + y + "," + z);
                    } else if (e.getValue() == EndBlock.MYCELIA_CAP && airBelow && capAbove) {
                        throw new AssertionError(
                            size + " seed " + seed + " bare underside at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    @Test
    void holdsTogether() {
        for (GiantMushroom size : GiantMushroom.values()) {
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                size.grow(new Random(seed), X, Y, Z, 40, v);
                assertEquals(v.cells.size(), v.connected(X, Y, Z), size + " seed " + seed);
            }
        }
    }

    @Test
    void sizesAreOrdered() {
        double[] mean = new double[3];
        for (GiantMushroom size : GiantMushroom.values()) {
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                size.grow(new Random(seed), X, Y, Z, 40, v);
                mean[size.ordinal()] += (v.height(Y) + 1) / 50.0;
            }
        }
        assertTrue(mean[0] < mean[1], "small " + mean[0] + " medium " + mean[1]);
        assertTrue(mean[1] < mean[2], "medium " + mean[1] + " large " + mean[2]);
    }

    @Test
    void shrinksToTheHeadroom() {
        int[] lowest = { 6, 12, 22 };
        for (GiantMushroom size : GiantMushroom.values()) {
            int headroom = lowest[size.ordinal()];
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                assertTrue(size.grow(new Random(seed), X, Y, Z, headroom, v), size + " seed " + seed);
                assertTrue(v.height(Y) < headroom, size + " seed " + seed);
            }
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(GiantMushroom.SMALL.grow(new Random(1), X, Y, Z, 5, v));
        assertTrue(v.isEmpty());
    }
}

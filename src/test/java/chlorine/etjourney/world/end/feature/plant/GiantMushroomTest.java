package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The MYCELIA giant mushrooms: three sizes with a stem, a gilled underside and a capped top. */
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

    @Test
    void gillsLineTheUnderside() {
        for (GiantMushroom size : GiantMushroom.values()) {
            for (int seed = 0; seed < 50; seed++) {
                Voxels v = new Voxels();
                size.grow(new Random(seed), X, Y, Z, 40, v);
                int columns = 0;
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        EndBlock lowest = null, highest = null;
                        for (int dy = 0; dy <= 31; dy++) {
                            EndBlock b = v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz));
                            if (b != EndBlock.MYCELIA_GILL && b != EndBlock.MYCELIA_CAP) continue;
                            if (lowest == null) lowest = b;
                            highest = b;
                        }
                        if (lowest == null) continue;
                        columns++;
                        assertEquals(EndBlock.MYCELIA_GILL, lowest, size + " seed " + seed);
                        assertEquals(EndBlock.MYCELIA_CAP, highest, size + " seed " + seed);
                    }
                }
                assertTrue(columns > 0);
                assertTrue(v.count(EndBlock.MYCELIA_STEM) > 0);
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

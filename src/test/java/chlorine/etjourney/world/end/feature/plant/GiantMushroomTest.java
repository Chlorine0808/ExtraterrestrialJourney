package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The MYCELIA giant mushrooms: the small one, a piece, plain on top with gills lining the underside. */
class GiantMushroomTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void onlySmallRemains() {
        assertEquals(1, GiantMushroom.values().length);
        assertEquals("SMALL", GiantMushroom.values()[0].name());
    }

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
    void smallGillsAreEven() {
        int columns = 0, gilled = 0;
        for (int i = 1; i <= 20; i++) {
            Voxels v = new Voxels();
            GiantMushroom.SMALL.grow(new Random(i * 0x9E3779B97F4A7C15L), X, Y, Z, 40, v);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    EndBlock lowest = null;
                    boolean cap = false, stem = false;
                    for (int dy = 0; dy <= 31; dy++) {
                        EndBlock b = v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz));
                        if (b == null) continue;
                        if (lowest == null) lowest = b;
                        cap |= isCap(b);
                        stem |= b == EndBlock.MYCELIA_STEM;
                    }
                    if (!cap || stem) continue;
                    columns++;
                    if (lowest == EndBlock.MYCELIA_GILL) gilled++;
                }
            }
        }
        assertTrue(columns > 0);
        assertTrue(gilled >= 0.8 * columns, "gill share " + gilled + "/" + columns);
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
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 50; seed++) {
            Voxels v = new Voxels();
            assertTrue(GiantMushroom.SMALL.grow(new Random(seed), X, Y, Z, 6, v), "seed " + seed);
            assertTrue(v.height(Y) < 6, "seed " + seed);
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(GiantMushroom.SMALL.grow(new Random(1), X, Y, Z, 5, v));
        assertTrue(v.isEmpty());
    }
}

package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The AMBER tall stem: one column of stem 6 to 14 blocks high, capped by the headroom. */
class TallStemTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void staysWithinEight() {
        assertTrue(TallStem.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(TallStem.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v), "seed " + seed);
            assertTrue(v.maxDistance(X, Z) <= TallStem.INSTANCE.radius(), "seed " + seed);
            assertTrue(v.minY >= Y, "seed " + seed);
        }
    }

    @Test
    void staysBelowThirty() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            TallStem.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            assertTrue(v.height(Y) <= 30, "seed " + seed);
        }
    }

    @Test
    void isOneSolidColumnSixToFourteenHigh() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            TallStem.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            int n = v.count(EndBlock.AMBER_STEM);
            assertEquals(v.cells.size(), n, "seed " + seed);
            assertTrue(n >= 6 && n <= 14, "seed " + seed);
            for (int dy = 0; dy < n; dy++) {
                assertEquals(EndBlock.AMBER_STEM, v.cells.get(Voxels.key(X, Y + dy, Z)), "seed " + seed);
            }
        }
    }

    @Test
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(TallStem.INSTANCE.grow(new Random(seed), X, Y, Z, 8, v), "seed " + seed);
            assertTrue(v.height(Y) <= 6, "seed " + seed);
        }
    }

    @Test
    void refusesUnderFourBlocks() {
        Voxels v = new Voxels();
        assertFalse(TallStem.INSTANCE.grow(new Random(1), X, Y, Z, 4, v));
        assertTrue(v.isEmpty());
        assertTrue(TallStem.INSTANCE.grow(new Random(1), X, Y, Z, 5, new Voxels()));
    }
}

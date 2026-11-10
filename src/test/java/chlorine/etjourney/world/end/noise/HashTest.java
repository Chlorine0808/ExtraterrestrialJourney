package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The lattice hashes stay in [0, 1), repeat for equal input and differ for any changed input. */
class HashTest {

    @Test
    void valuesStayInUnitRange() {
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                double v = Hash.hash01(99L, x, z);
                assertTrue(v >= 0 && v < 1, "out of range: " + v);
            }
        }
    }

    @Test
    void sameInputGivesSameValue() {
        assertEquals(Hash.hash01(5L, 3, -9), Hash.hash01(5L, 3, -9));
        assertEquals(Hash.hash01(5L, 3, 4, -9), Hash.hash01(5L, 3, 4, -9));
    }

    @Test
    void changedSeedCoordOrHeightGivesDifferentValue() {
        double base = Hash.hash01(5L, 3, 4, -9);
        assertNotEquals(base, Hash.hash01(6L, 3, 4, -9));
        assertNotEquals(base, Hash.hash01(5L, 4, 4, -9));
        assertNotEquals(base, Hash.hash01(5L, 3, 5, -9));
        assertNotEquals(base, Hash.hash01(5L, 3, 4, -8));
        // x and z are not interchangeable
        assertNotEquals(Hash.hash01(5L, 3, 8), Hash.hash01(5L, 8, 3));
    }

    @Test
    void neighbouringSeedsAreNotCorrelated() {
        int close = 0;
        for (int x = 0; x < 200; x++) {
            if (Math.abs(Hash.hash01(1000L, x, 0) - Hash.hash01(1001L, x, 0)) < 0.05) close++;
        }
        // Uniform values land within 0.05 of each other about 10% of the time.
        assertTrue(close < 40, "seeds 1000 and 1001 look correlated: " + close);
    }

    @Test
    void pinnedValuesCatchChangedGeneration() {
        assertEquals(0.3381666012719897, Hash.hash01(1L, 0, 0), 0);
        assertEquals(0.3613380723976113, Hash.hash01(42L, 3, -7), 0);
        assertEquals(0.8937118699428432, Hash.hash01(-5L, 100, 200), 0);
        assertEquals(0.5989484743011328, Hash.hash01(7L, 1, 2, 3), 0);
        assertEquals(0.9796323162087598, Hash.hash01(7L, -4, 60, 9), 0);
    }
}

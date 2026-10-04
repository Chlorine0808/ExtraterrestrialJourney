package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValueNoiseTest {

    @Test
    void hashIsDeterministicAndInRange() {
        for (int i = -50; i < 50; i++) {
            double h = Hash.hash01(42L, i, i * 7);
            assertTrue(h >= 0 && h < 1);
            assertEquals(h, Hash.hash01(42L, i, i * 7));
        }
        assertNotEquals(Hash.hash01(1L, 3, 4), Hash.hash01(2L, 3, 4));
    }

    @Test
    void neighbouringSeedsAreNotCorrelated() {
        int close = 0;
        for (int i = 0; i < 1000; i++) {
            if (Math.abs(Hash.hash01(100L, i, 0) - Hash.hash01(101L, i, 0)) < 0.05) close++;
        }
        assertTrue(close < 150, "too many close pairs: " + close);
    }

    @Test
    void maskIsContinuousAndInRange() {
        double prev = ValueNoise.mask(7L, 0, 0, 32);
        for (int x = 1; x < 500; x++) {
            double v = ValueNoise.mask(7L, x, 0, 32);
            assertTrue(v >= 0 && v <= 1);
            assertTrue(Math.abs(v - prev) < 0.1, "jump at " + x);
            prev = v;
        }
    }

    @Test
    void noise3IsInRange() {
        for (int i = 0; i < 200; i++) {
            double v = ValueNoise.noise3(9L, i * 1.7, i * 0.3, -i * 2.1, 9);
            assertTrue(v >= 0 && v <= 1);
        }
    }
}

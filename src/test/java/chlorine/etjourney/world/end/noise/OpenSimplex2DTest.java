package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The 2D noise stays within [-1, 1], is deterministic per seed, continuous, and pinned to its current output. */
class OpenSimplex2DTest {

    @Test
    void valuesStayInRangeAndUseBothSigns() {
        OpenSimplex2D n = new OpenSimplex2D(7L);
        double min = 1, max = -1;
        for (int i = 0; i < 4000; i++) {
            double v = n.eval(i * 0.37 - 300, i * 0.61 - 500);
            assertTrue(v >= -1 && v <= 1, "out of range: " + v);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        assertTrue(min < -0.3 && max > 0.3, "spread too narrow: " + min + " .. " + max);
    }

    @Test
    void sameSeedRepeatsAndOtherSeedDiffers() {
        assertEquals(new OpenSimplex2D(3L).eval(4.2, 9.9), new OpenSimplex2D(3L).eval(4.2, 9.9));
        assertNotEquals(new OpenSimplex2D(3L).eval(4.2, 9.9), new OpenSimplex2D(4L).eval(4.2, 9.9));
    }

    @Test
    void smallStepsGiveSmallChanges() {
        OpenSimplex2D n = new OpenSimplex2D(11L);
        double step = 0.01;
        for (int i = 0; i < 2000; i++) {
            double x = i * 0.173 - 150, y = i * 0.091 + 20;
            assertTrue(Math.abs(n.eval(x + step, y) - n.eval(x, y)) < 0.1, "jump along x at " + x + "," + y);
            assertTrue(Math.abs(n.eval(x, y + step) - n.eval(x, y)) < 0.1, "jump along y at " + x + "," + y);
        }
    }

    @Test
    void pinnedValuesCatchChangedGeneration() {
        OpenSimplex2D n = new OpenSimplex2D(12345L);
        assertEquals(0.0, n.eval(0, 0), 0);
        assertEquals(-0.5821832962921272, n.eval(1.5, -2.25), 1e-12);
        assertEquals(-0.1567011127271384, n.eval(10.1, 20.2), 1e-12);
        assertEquals(-0.19921394405669746, n.eval(-33.3, 7.7), 1e-12);
    }
}

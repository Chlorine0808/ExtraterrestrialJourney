package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FractalTest {

    @Test
    void fbmAndRidgedStayInUnitRange() {
        for (int i = 0; i < 500; i++) {
            double f = Fractal.fbm(3L, i * 13.1, -i * 7.7, 40, 4);
            double r = Fractal.ridged(3L, i * 13.1, -i * 7.7, 40, 4);
            assertTrue(f >= 0 && f <= 1 && r >= 0 && r <= 1);
        }
    }

    @Test
    void warpMovesNoFurtherThanItsAmplitude() {
        for (int i = 0; i < 500; i++) {
            double[] w = Warp.warp(5L, i * 3.0, i * -2.0, 28, 110);
            assertTrue(Math.hypot(w[0] - i * 3.0, w[1] + i * 2.0) <= 28 * Math.sqrt(2) + 1e-9);
        }
    }
}

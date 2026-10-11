package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Distance.planar stands in for Math.hypot over the coordinates terrain works with. */
class DistanceTest {

    @Test
    void agreesWithHypot() {
        Random r = new Random(1);
        for (int n = 0; n < 100000; n++) {
            double dx = (r.nextDouble() - 0.5) * 2e5, dz = (r.nextDouble() - 0.5) * 2e5;
            double expected = Math.hypot(dx, dz);
            assertEquals(expected, Distance.planar(dx, dz), expected * 1e-15);
        }
    }

    @Test
    void zeroAndAxes() {
        assertEquals(0, Distance.planar(0, 0));
        assertEquals(5, Distance.planar(-5, 0));
        assertEquals(5, Distance.planar(3, -4));
    }
}

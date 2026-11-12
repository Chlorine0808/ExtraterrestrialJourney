package chlorine.etjourney.content.worm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A leap peaks at the asked height and comes back down at the asked distance, tick by tick. */
class LeapArcTest {

    private static final double G = 0.08;

    /** Flies the launch the way the worm does (move, then pull down) and returns {peak, landing distance}. */
    private static double[] fly(double[] v) {
        double x = 0, y = 0, z = 0, peak = 0, vy = v[1];
        do {
            x += v[0];
            y += vy;
            z += v[2];
            vy -= G;
            peak = Math.max(peak, y);
        } while (y > 0);
        return new double[] { peak, Math.sqrt(x * x + z * z) };
    }

    @Test
    void theLeapPeaksAtTheAskedHeight() {
        double[] flight = fly(LeapArc.launch(1, 0, 16, 8, G));
        assertEquals(8, flight[0], 0.1);
    }

    @Test
    void theLeapComesDownTheAskedDistanceAwayAlongTheDirection() {
        double[] v = LeapArc.launch(3, 4, 16, 8, G);
        double[] flight = fly(v);
        double perTick = Math.hypot(v[0], v[2]);
        assertEquals(16, flight[1], perTick);
        assertEquals(3.0 / 4.0, v[0] / v[2], 1e-9);
    }

    @Test
    void aLeapOnTheSpotGoesStraightUp() {
        double[] v = LeapArc.launch(0, 0, 0, 5, G);
        assertEquals(0, v[0], 1e-9);
        assertEquals(0, v[2], 1e-9);
        assertEquals(5, fly(v)[0], 0.1);
    }
}

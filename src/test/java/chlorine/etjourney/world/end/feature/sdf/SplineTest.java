package chlorine.etjourney.world.end.feature.sdf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Splines: evenly spaced points, jittered, and drawn as a chain of tapering capsules. */
class SplineTest {

    private static final double EPS = 1e-9;

    @Test
    void makeSpacesThePointsEvenly() {
        List<double[]> s = Spline.make(0, 0, 0, 0, 12, 0, 4);
        assertEquals(4, s.size());
        assertArrayEquals(new double[] { 0, 0, 0 }, s.get(0), EPS);
        assertArrayEquals(new double[] { 0, 4, 0 }, s.get(1), EPS);
        assertArrayEquals(new double[] { 0, 12, 0 }, s.get(3), EPS);
    }

    @Test
    void jitterKeepsTheFirstPointAndClampsTheRest() {
        List<double[]> s = Spline.make(0, 0, 0, 0, 20, 0, 6);
        Spline.jitter(s, new Random(3), 5, 0, 5, 1.5);
        assertArrayEquals(new double[] { 0, 0, 0 }, s.get(0), EPS);
        boolean moved = false;
        for (double[] p : s) {
            assertTrue(Math.abs(p[0]) <= 1.5 + EPS && Math.abs(p[2]) <= 1.5 + EPS);
            moved |= p[0] != 0;
        }
        assertTrue(moved);
    }

    @Test
    void shapeTapersFromTheFirstRadiusToTheLast() {
        Sdf trunk = Spline.shape(Spline.make(0, 0, 0, 0, 20, 0, 5), 3, 1);
        assertTrue(trunk.distance(2.5, 0.5, 0) < 0);
        assertTrue(trunk.distance(2.5, 19.5, 0) > 0);
        assertTrue(trunk.distance(0.5, 19.5, 0) < 0);
    }
}

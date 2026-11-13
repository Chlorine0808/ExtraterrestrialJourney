package chlorine.etjourney.content.worm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A worm turns towards where it wants to go by at most a set angle a tick, so its path never folds back. */
class SteeringTest {

    private static final double EPS = 1e-9;

    private static double angle(double[] a, double[] b) {
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.acos(Math.max(-1, Math.min(1, dot)));
    }

    @Test
    void aSmallTurnIsMadeInFull() {
        double[] want = { Math.cos(0.1), 0, Math.sin(0.1) };
        assertArrayEquals(want, Steering.turn(new double[] { 1, 0, 0 }, want, 0.2), EPS);
    }

    @Test
    void aSharpTurnIsCutToTheLimit() {
        double[] dir = Steering.turn(new double[] { 1, 0, 0 }, new double[] { 0, 0, 1 }, 0.2);
        assertEquals(0.2, angle(dir, new double[] { 1, 0, 0 }), 1e-6);
        assertEquals(1, Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]), 1e-9);
    }

    @Test
    void turningRightAroundStillTurnsByTheLimitInsteadOfStopping() {
        double[] dir = Steering.turn(new double[] { 1, 0, 0 }, new double[] { -1, 0, 0 }, 0.2);
        assertEquals(0.2, angle(dir, new double[] { 1, 0, 0 }), 1e-6);
    }

    @Test
    void noHeadingYetTakesTheWantedOne() {
        assertArrayEquals(
            new double[] { 0, 1, 0 },
            Steering.turn(new double[] { 0, 0, 0 }, new double[] { 0, 3, 0 }, 0.2),
            EPS);
    }
}

package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** One tick of a shot's velocity: the homing turn, then gravity, then drag. */
class ProjectileMotionTest {

    private static final double EPS = 1e-9;
    private static final Homing HOMING = new Homing(0.2, 2, 3, 16);

    private static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double angle(double[] a, double[] b) {
        double dot = (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) / (norm(a) * norm(b));
        return Math.acos(Math.max(-1, Math.min(1, dot)));
    }

    @Test
    void gravityPullsDownAndDragSlowsAfterIt() {
        double[] v = ProjectileMotion.step(new double[] { 1, 0, 0 }, 0.1, 0.5, null, 0, null);
        assertArrayEquals(new double[] { 0.5, -0.05, 0 }, v, EPS);
    }

    @Test
    void homingTurnsByTheRateAndKeepsTheSpeed() {
        double[] before = { 0.5, 0, 0 };
        double[] v = ProjectileMotion.step(before, 0, 1, HOMING, 2, new double[] { 0, 0, 10 });
        assertEquals(0.5, norm(v), EPS);
        assertEquals(0.2, angle(before, v), 1e-6);
    }

    @Test
    void doesNotTurnOutsideTheWindowOrWithoutATarget() {
        double[] before = { 0.5, 0, 0 };
        double[] toTarget = { 0, 0, 10 };
        assertArrayEquals(before, ProjectileMotion.step(before, 0, 1, HOMING, 1, toTarget), EPS);
        assertArrayEquals(before, ProjectileMotion.step(before, 0, 1, HOMING, 5, toTarget), EPS);
        assertArrayEquals(before, ProjectileMotion.step(before, 0, 1, HOMING, 2, null), EPS);
    }

    @Test
    void leavesTheGivenVelocityAlone() {
        double[] before = { 1, 0, 0 };
        ProjectileMotion.step(before, 0.1, 0.5, HOMING, 2, new double[] { 0, 0, 1 });
        assertArrayEquals(new double[] { 1, 0, 0 }, before, EPS);
    }
}

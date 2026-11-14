package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Directions for firing several shots at once. */
class SpreadTest {

    private static final double EPS = 1e-9;

    private static double yaw(double[] v) {
        return Math.atan2(v[2], v[0]);
    }

    private static double angle(double[] a, double[] b) {
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.acos(Math.max(-1, Math.min(1, dot)));
    }

    @Test
    void aFanSpreadsEvenlyAcrossTheAngleAroundTheVertical() {
        List<double[]> fan = Spread.fan(new double[] { 1, 0, 0 }, 3, Math.toRadians(60));
        assertEquals(3, fan.size());
        assertEquals(Math.toRadians(-30), yaw(fan.get(0)), 1e-6);
        assertEquals(0, yaw(fan.get(1)), 1e-6);
        assertEquals(Math.toRadians(30), yaw(fan.get(2)), 1e-6);
    }

    @Test
    void aFanKeepsTheClimbOfTheMiddle() {
        for (double[] d : Spread.fan(new double[] { 1, 1, 0 }, 4, 1.0)) assertEquals(Math.sqrt(0.5), d[1], EPS);
    }

    @Test
    void aFanOfOneIsTheDirectionItself() {
        assertArrayEquals(
            new double[] { 0, 0, 1 },
            Spread.fan(new double[] { 0, 0, 2 }, 1, 1.0)
                .get(0),
            EPS);
    }

    @Test
    void aConeStaysInsideItsAngle() {
        Random random = new Random(7);
        double[] axis = { 0, 1, 0 };
        double widest = 0;
        for (int i = 0; i < 500; i++) {
            double[] d = Spread.cone(axis, 0.4, random);
            assertEquals(1, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9);
            double a = angle(axis, d);
            assertTrue(a <= 0.4 + 1e-9, "outside the cone: " + a);
            widest = Math.max(widest, a);
        }
        assertTrue(widest > 0.3, "the cone is not filled: " + widest);
    }
}

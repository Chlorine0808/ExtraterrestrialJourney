package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The launch velocity that sends a shot at a point, straight or in an arc. */
class AimTest {

    private static final double EPS = 1e-9;

    @Test
    void straightPointsAtTheTargetAtTheGivenSpeed() {
        assertArrayEquals(new double[] { 0, 0, 2 }, Aim.straight(0, 0, 5, 2), EPS);
        double[] v = Aim.straight(3, 4, 0, 1);
        assertArrayEquals(new double[] { 0.6, 0.8, 0 }, v, EPS);
    }

    /** Moves then pulls each tick, as the shot does, and returns the horizontal distance where it falls to dy. */
    private static double[] land(double[] v, double dy, double gravity) {
        double x = 0, y = 0, z = 0, peak = 0;
        double vx = v[0], vy = v[1], vz = v[2];
        for (int t = 0; t < 10_000; t++) {
            double ny = y + vy;
            if (vy < 0 && ny <= dy) {
                double f = (y - dy) / (y - ny);
                return new double[] { Math.hypot(x + vx * f, z + vz * f), peak };
            }
            x += vx;
            y = ny;
            z += vz;
            peak = Math.max(peak, y);
            vy -= gravity;
        }
        throw new AssertionError("never landed");
    }

    @Test
    void aLobComesDownOnTheTargetOnFlatGround() {
        double[] v = Aim.lob(12, 0, 0, 3, 0.05);
        double[] landed = land(v, 0, 0.05);
        assertEquals(12, landed[0], 0.05);
        assertEquals(3, landed[1], 0.05);
    }

    @Test
    void aLobReachesATargetAboveOrBelow() {
        assertEquals(10, land(Aim.lob(6, 5, 8, 2, 0.05), 5, 0.05)[0], 0.05);
        assertEquals(10, land(Aim.lob(6, -7, 8, 2, 0.05), -7, 0.05)[0], 0.05);
    }

    @Test
    void aLobOnTheSpotGoesStraightUp() {
        double[] v = Aim.lob(0, 0, 0, 3, 0.05);
        assertEquals(0, v[0], EPS);
        assertEquals(0, v[2], EPS);
    }
}

package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.debug.EtjArgs;
import chlorine.etjourney.world.end.feature.EndBiome;

class FramingTest {

    @Test
    void aerialCameraLooksAtTheSubject() {
        Target t = new Target("t", 1000, 80, -500, 96, 0);
        Camera c = Framing.aerial(t, 0);
        double[] d = EtjArgs.lookDirection(c.yaw, c.pitch);
        double k = Framing.subjectDistance(t);
        assertArrayEquals(
            new double[] { t.x, t.y, t.z },
            new double[] { c.x + d[0] * k, c.y + Framing.EYE + d[1] * k, c.z + d[2] * k },
            0.5);
        assertEquals(240, k, 1e-9);
    }

    @Test
    void aerialRetryTurnsAndBacksOff() {
        Target t = new Target("t", 0, 80, 0, 96, 0);
        Camera a = Framing.aerial(t, 0), b = Framing.aerial(t, 1);
        assertEquals(90, Math.floorMod((int) (b.yaw - a.yaw), 360));
        assertTrue(Math.hypot(b.x - t.x, b.z - t.z) > Math.hypot(a.x - t.x, a.z - t.z));
    }

    @Test
    void lateAerialTriesLookDownSteeplyBetweenTheEarlierOnes() {
        // 20261029-040906, 36 (ARCS touch): the three 35-degree tries were blocked by tubes 92 and 112 blocks out
        // and buried. Steeper looks clear what stands around the subject.
        Target t = new Target("t", 0, 80, 0, 64, 0);
        assertEquals(6, Framing.AERIAL_ATTEMPTS);
        for (int i = 3; i < Framing.AERIAL_ATTEMPTS; i++) {
            Camera c = Framing.aerial(t, i);
            assertEquals(60, c.pitch, 1e-6);
            assertEquals(45, Math.floorMod((int) c.yaw, 90));
            double[] d = EtjArgs.lookDirection(c.yaw, c.pitch);
            double k = Framing.subjectDistance(t);
            assertEquals(t.y, c.y + Framing.EYE + d[1] * k, 0.5);
        }
    }

    @Test
    void distanceStaysWithinLimits() {
        assertEquals(48, Framing.subjectDistance(new Target("s", 0, 0, 0, 4, 0)), 1e-9);
        assertEquals(400, Framing.subjectDistance(new Target("l", 0, 0, 0, 1000, 0)), 1e-9);
    }

    @Test
    void outsideCameraLooksAtTheBallCentre() {
        Target ball = new Target("b", 100, 120, 200, 50, 0);
        Camera c = Framing.outside(ball, 0);
        double[] d = EtjArgs.lookDirection(c.yaw, c.pitch);
        double k = 50 * 2.6;
        assertArrayEquals(
            new double[] { ball.x, ball.y, ball.z },
            new double[] { c.x + d[0] * k, c.y + Framing.EYE + d[1] * k, c.z + d[2] * k },
            0.5);
        assertEquals(20, c.pitch, 1e-6);
    }

    @Test
    void groundYawFacesTheBiome() {
        List<Float> yaws = Framing
            .groundYaws((x, z) -> x > 0 && Math.abs(z) < 10 ? EndBiome.GROVE : null, 0, 0, EndBiome.GROVE);
        assertEquals(8, yaws.size());
        assertTrue(EtjArgs.lookDirection(yaws.get(0), 0)[0] > 0.9); // faces +X
    }

    @Test
    void groundCameraStandsOnTheGround() {
        Camera c = Framing.ground(1, 70, 2, 45f);
        assertEquals(70, c.y, 1e-9);
        assertEquals(8, c.pitch, 1e-6);
        assertEquals(45, c.yaw, 1e-6);
    }

    @Test
    void clearViewNeedsTheHitNearTheSubject() {
        assertTrue(Framing.clear(230, 240, View.AERIAL));
        assertFalse(Framing.clear(20, 240, View.AERIAL));
        assertTrue(Framing.clear(Double.NaN, 240, View.AERIAL));
        assertFalse(Framing.clear(5, 0, View.GROUND));
        assertTrue(Framing.clear(30, 0, View.INSIDE));
    }

    @Test
    void groundHasASecondSpotFurtherIntoTheBiome() {
        // When every look from the target's own column fails (08 in 20261029-034306 stood on a cliff ledge),
        // the next tries stand further along the best look, into the biome.
        List<double[]> spots = Framing.groundSpots(10, 20, 270f);
        assertEquals(2, spots.size());
        assertArrayEquals(new double[] { 10, 20 }, spots.get(0), 1e-9);
        assertEquals(10 + Framing.GROUND_SHIFT, spots.get(1)[0], 1e-9); // yaw 270 faces +X
        assertEquals(20, spots.get(1)[1], 1e-9);
    }

    @Test
    void groundColumnsStartAtTheTargetAndRingIt() {
        List<double[]> cols = Framing.groundColumns(100, 200);
        assertArrayEquals(new double[] { 100, 200 }, cols.get(0), 1e-9);
        assertEquals(17, cols.size());
        for (double[] c : cols) assertTrue(Math.hypot(c[0] - 100, c[1] - 200) <= 16 + 1e-9);
    }

    @Test
    void groundIsTheFloorNearestTheExpectedTop() {
        // 20261029-034306, 06/08/12: with no floor within 24 blocks under the expected top, the camera was left
        // floating in the air (08 beside a cliff, looking into the void). Columns without a floor are NaN.
        assertEquals(-1, Framing.pickGround(57, new double[] { Double.NaN, Double.NaN }));
        assertEquals(1, Framing.pickGround(57, new double[] { Double.NaN, 50, 70 }));
        assertEquals(2, Framing.pickGround(57, new double[] { 20, 80, 60 }));
        assertEquals(0, Framing.pickGround(57, new double[] { 55, 59 })); // a tie keeps the earlier
    }
}

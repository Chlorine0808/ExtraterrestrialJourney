package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.debug.EtjArgs;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.plant.GiantTrees;

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

    @Test
    void groundFarFromTheExpectedTopIsNotTheContinent() {
        // 20261029-045518, 06: with the whole column searched, the nearest "ground" in every highland column was an
        // obsidian pad in the void 123 blocks under the expected top, and the camera stood on it.
        assertEquals(-1, Framing.pickGround(146, new double[] { 23, 146 - Framing.GROUND_REACH - 1 }));
        assertEquals(1, Framing.pickGround(146, new double[] { 23, 146 - Framing.GROUND_REACH }));
    }

    private static final Target SEED_TREE = new Target("t", 100, 81, -40, 40, 0);

    @Test
    void treeGroundStandsOffAlongPlusXOnFlatGround() {
        Camera c = Framing.treeGround(SEED_TREE, (x, z) -> 80);
        assertEquals(SEED_TREE.x + 28, c.x, 1e-9);
        assertEquals(SEED_TREE.z, c.z, 1e-9);
        assertEquals(SEED_TREE.y + 4, c.y, 1e-9);
        assertEquals(90f, c.yaw, 1e-6);
        assertEquals(-9, c.pitch, 0.5);
    }

    @Test
    void treeGroundRisesAboveARisingSlope() {
        // Ground climbs 0.6 per block east of the tree, 9 blocks above the root where the camera stood.
        Framing.GroundAt slope = (x, z) -> 80 + Math.max(0, x - SEED_TREE.x) * 0.6;
        Camera c = Framing.treeGround(SEED_TREE, slope);
        double[] d = EtjArgs.lookDirection(c.yaw, 0);
        for (int r = 0; r <= 14; r++) {
            double x = c.x + d[0] * r, z = c.z + d[2] * r;
            assertTrue(c.y >= Math.floor(slope.at(x, z)) + 1, "buried " + r + " blocks in front");
        }
        assertLooksAtTheTree(c);
    }

    @Test
    void treeGroundStandsOnLandAtAContinentsEdge() {
        // The void starts five blocks east of the tree.
        Framing.GroundAt edge = (x, z) -> x > SEED_TREE.x + 4 ? -1000 : 80;
        Camera c = Framing.treeGround(SEED_TREE, edge);
        assertTrue(c.x <= SEED_TREE.x + 4, "x " + c.x);
        assertEquals(SEED_TREE.y + 4, c.y, 1e-9);
        assertLooksAtTheTree(c);
    }

    @Test
    void treeGroundsTryEveryTurnThatStandsOnLandFirst() {
        // The void starts five blocks east of the tree: the views that stand on land come before the east one.
        Framing.GroundAt edge = (x, z) -> x > SEED_TREE.x + 4 ? -1000 : 80;
        List<Camera> tries = Framing.treeGrounds(SEED_TREE, edge, Framing.SEED_TREE);
        assertEquals(8, tries.size());
        assertEquals(Framing.treeGround(SEED_TREE, edge).x, tries.get(0).x, 1e-9);
        boolean overVoid = false;
        for (Camera c : tries) {
            boolean onLand = c.x <= SEED_TREE.x + 4;
            assertTrue(!(overVoid && onLand), "a view on land came after one over the void");
            overVoid |= !onLand;
            assertLooksAtTheTree(c);
        }
        assertTrue(overVoid);
    }

    @Test
    void giantTreeGroundSeesTheRootAndTheTop() {
        // 20261109-021434, 2: the continent ground view stood among the trunks and looked away from the tree.
        Target tree = new Target("t", 0, 90, 0, 40, 0);
        Camera c = Framing.treeGrounds(tree, (x, z) -> 90, Framing.GIANT_TREE)
            .get(0);
        double dist = Math.hypot(c.x - tree.x, c.z - tree.z);
        assertTrue(dist > GiantTrees.FOOTPRINT, "inside the crown at " + dist);
        double eye = c.y + Framing.EYE;
        double toRoot = Math.toDegrees(Math.atan2(eye - tree.y, dist));
        double toTop = Math.toDegrees(Math.atan2(eye - (tree.y + GiantTrees.HEIGHT), dist));
        // Pitch is positive downward; the vertical field of view is 70 degrees.
        assertTrue(toRoot - c.pitch < 35 && c.pitch - toTop < 35, "pitch " + c.pitch);
    }

    @Test
    void treeAerialsCircleTheAimThenLookDownSteeper() {
        // 20261109-014627: one fixed aerial 18 blocks off was buried twice and showed a tangle of legs the rest.
        Target aim = new Target("t", 10, 90, -20, 40, 0);
        List<Camera> tries = Framing.treeAerials(aim, 32);
        assertEquals(8, tries.size());
        assertEquals(90f, tries.get(0).yaw, 1e-6);
        for (int i = 0; i < tries.size(); i++) {
            Camera c = tries.get(i);
            double[] d = EtjArgs.lookDirection(c.yaw, c.pitch);
            assertArrayEquals(
                new double[] { aim.x, aim.y, aim.z },
                new double[] { c.x + d[0] * 32, c.y + Framing.EYE + d[1] * 32, c.z + d[2] * 32 },
                0.5);
            assertTrue(i < 4 ? c.pitch < tries.get(4).pitch : Math.floorMod((int) c.yaw, 90) == 45);
        }
    }

    private static void assertLooksAtTheTree(Camera c) {
        double[] d = EtjArgs.lookDirection(c.yaw, 0);
        double dist = Math.hypot(SEED_TREE.x - c.x, SEED_TREE.z - c.z);
        assertEquals(SEED_TREE.x, c.x + d[0] * dist, 0.5);
        assertEquals(SEED_TREE.z, c.z + d[2] * dist, 0.5);
    }
}

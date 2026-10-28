package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PocketTest {

    @Test
    void solidBallHasNoPocket() {
        assertTrue(
            Pocket.rank((x, y, z) -> false, 0, 0, 0, 40, 40, 40, 20, 20, 3)
                .isEmpty());
    }

    @Test
    void flatFloorGivesAStandingSpotFacingTheOpenSide() {
        // Floor at y 10 inside a 41-wide box, open everywhere above it.
        List<Camera> spots = Pocket.rank((x, y, z) -> y > 10, 0, 0, 0, 40, 40, 40, 20, 20, 3);
        assertEquals(3, spots.size());
        assertEquals(11, spots.get(0).y, 0);
        assertTrue(Math.hypot(spots.get(0).x - 20, spots.get(0).z - 20) < 6);
        assertEquals(10f, spots.get(0).pitch, 0);
    }

    @Test
    void treeTopsAreNotStandingSpots() {
        // Ground at y 10, and a canopy of leaves at y 20 over x < 20 that is more open than the ground below it.
        Pocket.Grid g = new Pocket.Grid() {

            @Override
            public boolean air(int x, int y, int z) {
                return y > 10 && !noFooting(x, y, z);
            }

            @Override
            public boolean noFooting(int x, int y, int z) {
                return y == 20 && x < 20;
            }
        };
        List<Camera> spots = Pocket.rank(g, 0, 0, 0, 40, 40, 40, 20, 20, 200);
        assertTrue(!spots.isEmpty());
        for (Camera c : spots) assertEquals(11, c.y, 0);
    }

    @Test
    void lavaIsNoGround() {
        // 20261029-040906, 34 (nether ball 2, inside): the camera stood on a lava sea, whose animation kept the
        // thumbnails from ever settling, so the shot timed out. Lava fills y <= 10 for x < 30; rock elsewhere.
        Pocket.Grid g = new Pocket.Grid() {

            @Override
            public boolean air(int x, int y, int z) {
                return y > 10;
            }

            @Override
            public boolean noFooting(int x, int y, int z) {
                return y <= 10 && x < 30;
            }
        };
        List<Camera> spots = Pocket.rank(g, 0, 0, 0, 40, 40, 40, 20, 20, 200);
        assertTrue(!spots.isEmpty());
        for (Camera c : spots) assertTrue(c.x >= 30, "stood on lava at x " + c.x);
    }

    @Test
    void yawFacesTheLongestOpenDirection() {
        // A corridor along +z (yaw 0): air only where x == 5 and y > 10.
        List<Camera> spots = Pocket.rank((x, y, z) -> y > 10 && x == 5, 0, 0, 0, 10, 40, 60, 5, 5, 200);
        assertTrue(!spots.isEmpty());
        Camera c = spots.get(0);
        assertTrue(c.yaw == 0f || c.yaw == 180f);
    }

    @Test
    void spreadSkipsNearDuplicateSpots() {
        List<Camera> ranked = Arrays.asList(
            new Camera(0.5, 11, 0.5, 0, 10),
            new Camera(1.5, 11, 0.5, 0, 10),
            new Camera(12.5, 11, 0.5, 0, 10),
            new Camera(13.5, 11, 0.5, 0, 10),
            new Camera(0.5, 11, 20.5, 0, 10));
        List<Camera> picked = Pocket.spread(ranked, 3, 8);
        assertEquals(3, picked.size());
        assertEquals(0.5, picked.get(0).x, 0);
        assertEquals(12.5, picked.get(1).x, 0);
        assertEquals(20.5, picked.get(2).z, 0);
    }

    @Test
    void spreadReturnsFewerWhenAllSpotsAreClose() {
        List<Camera> ranked = Arrays.asList(new Camera(0.5, 11, 0.5, 0, 10), new Camera(1.5, 11, 0.5, 0, 10));
        assertEquals(
            1,
            Pocket.spread(ranked, 3, 8)
                .size());
    }
}

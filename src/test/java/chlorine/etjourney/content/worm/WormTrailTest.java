package chlorine.etjourney.content.worm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Segments sit on the path the head took, a fixed distance back along it. */
class WormTrailTest {

    private static final double EPS = 1e-9;

    @Test
    void aPointBackAlongAStraightPathIsThatFarBehindTheHead() {
        WormTrail trail = new WormTrail(10, 100);
        for (int x = 0; x <= 5; x++) trail.record(x, 64, 0);
        assertArrayEquals(new double[] { 5, 64, 0 }, trail.at(0), EPS);
        assertArrayEquals(new double[] { 2.5, 64, 0 }, trail.at(2.5), EPS);
    }

    @Test
    void thePathBendsRoundTheCornersTheHeadTurned() {
        WormTrail trail = new WormTrail(10, 100);
        trail.record(0, 0, 0);
        trail.record(2, 0, 0);
        trail.record(2, 0, 2);
        // 1 back from (2,0,2) is still on the second leg; 3 back is past the corner, on the first.
        assertArrayEquals(new double[] { 2, 0, 1 }, trail.at(1), EPS);
        assertArrayEquals(new double[] { 1, 0, 0 }, trail.at(3), EPS);
    }

    @Test
    void pastTheEndOfAShortTrailIsItsOldestPoint() {
        WormTrail trail = new WormTrail(10, 100);
        trail.record(0, 0, 0);
        trail.record(1, 0, 0);
        assertArrayEquals(new double[] { 0, 0, 0 }, trail.at(7), EPS);
    }

    @Test
    void pointsBeyondTheLengthAreDroppedWithoutMovingTheSegments() {
        WormTrail trail = new WormTrail(3, 100);
        for (int x = 0; x <= 50; x++) trail.record(x, 0, 0);
        assertTrue(trail.size() <= 6, "kept " + trail.size());
        assertArrayEquals(new double[] { 47, 0, 0 }, trail.at(3), EPS);
    }

    @Test
    void layingAStraightTrailPutsTheSegmentsBehindTheFacing() {
        WormTrail trail = new WormTrail(4, 100);
        trail.lay(10, 5, 10, 0, 0, 1);
        assertArrayEquals(new double[] { 10, 5, 7 }, trail.at(3), EPS);
        assertArrayEquals(new double[] { 10, 5, 6 }, trail.at(4), EPS);
    }

    @Test
    void aJumpRelaysTheTrailBehindTheNewSpotInsteadOfStretchingIt() {
        WormTrail trail = new WormTrail(4, 3);
        for (int x = 0; x <= 4; x++) trail.record(x, 0, 0);
        trail.record(100, 0, 50);
        // Still heading +x, so the segments line up behind the new head along -x.
        assertArrayEquals(new double[] { 98, 0, 50 }, trail.at(2), EPS);
    }
}

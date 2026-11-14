package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A floor rises from where it was put; a cloud spreads above and below; headroom stretches the reach upward only;
 * sideways a box counts by its edge.
 */
class AreaShapeTest {

    @Test
    void aFloorReachesFromItsBaseUpToItsHeight() {
        // Floor at y 4, 0.5 thick; a box standing on it from y 4 to 5.8.
        assertTrue(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 0, 0, 0.3, 4, 5.8));
        // A box hovering a block above the floor.
        assertFalse(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 0, 0, 0.3, 5, 6.8));
        // A box below the floor's base.
        assertFalse(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 0, 0, 0.3, 2, 3.9));
    }

    @Test
    void aCloudReachesHalfItsHeightAboveAndBelowItsCentre() {
        assertTrue(AreaShape.CLOUD.overlaps(2, 2, 0, 0, 10, 0, 0, 0, 0.3, 8.5, 9.5));
        assertTrue(AreaShape.CLOUD.overlaps(2, 2, 0, 0, 10, 0, 0, 0, 0.3, 10.9, 12));
        assertFalse(AreaShape.CLOUD.overlaps(2, 2, 0, 0, 10, 0, 0, 0, 0.3, 11.1, 12));
        assertFalse(AreaShape.CLOUD.overlaps(2, 2, 0, 0, 10, 0, 0, 0, 0.3, 7, 8.9));
    }

    @Test
    void sidewaysABoxCountsOnceItsEdgeCrossesTheRim() {
        // Centre 2.2 away, half width 0.3: its edge is 1.9 in, inside a radius of 2.
        assertTrue(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 2.2, 0, 0.3, 4, 5));
        assertFalse(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 2.4, 0, 0.3, 4, 5));
        // Diagonal distance counts, not each axis on its own.
        assertFalse(AreaShape.FLOOR.overlaps(2, 0.5, 0, 0, 4, 0, 1.8, 1.8, 0.3, 4, 5));
    }

    @Test
    void headroomReachesABoxStandingHigherThanTheFloor() {
        // Floor at y 4, 0.5 thick with 1 of headroom: a box on a block beside it, or mid-jump, still counts.
        assertTrue(AreaShape.FLOOR.overlaps(2, 0.5, 1, 0, 4, 0, 0, 0, 0.3, 5, 6.8));
        assertTrue(AreaShape.FLOOR.overlaps(2, 0.5, 1, 0, 4, 0, 0, 0, 0.3, 5.5, 7.3));
        assertFalse(AreaShape.FLOOR.overlaps(2, 0.5, 1, 0, 4, 0, 0, 0, 0.3, 5.6, 7.4));
    }

    @Test
    void headroomDoesNotReachDownward() {
        assertFalse(AreaShape.CLOUD.overlaps(2, 2, 1, 0, 10, 0, 0, 0, 0.3, 7, 8.9));
        assertTrue(AreaShape.CLOUD.overlaps(2, 2, 1, 0, 10, 0, 0, 0, 0.3, 11.9, 13));
    }
}

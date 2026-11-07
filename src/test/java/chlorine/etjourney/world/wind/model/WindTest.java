package chlorine.etjourney.world.wind.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** A wind catches a box overlapping it and speeds it along its direction until the cap. */
class WindTest {

    private static final double EPS = 1e-9;

    private static Wind column(double lift, double maxSpeed) {
        return new Wind(0, 0, 0, 4, 10, 4, lift, maxSpeed);
    }

    @Test
    void catchesABoxThatOverlapsIt() {
        Wind u = column(0.1, 0.5);
        assertTrue(u.overlaps(1, 2, 1, 1.6, 3.8, 1.6));
        // Reaching in from outside at one corner is enough.
        assertTrue(u.overlaps(-0.5, -1, -0.5, 0.1, 0.8, 0.1));
    }

    @Test
    void missesABoxThatOnlyTouchesItsFace() {
        Wind u = column(0.1, 0.5);
        // Standing on top of the column, or flush against its side, is not inside it.
        assertFalse(u.overlaps(1, 10, 1, 1.6, 11.8, 1.6));
        assertFalse(u.overlaps(4, 2, 1, 4.6, 3.8, 1.6));
    }

    private static Wind toward(double dx, double dy, double dz, double lift, double maxSpeed) {
        return new Wind(0, 0, 0, 4, 10, 4, dx, dy, dz, lift, maxSpeed);
    }

    private static void assertMotion(double x, double y, double z, double[] motion) {
        assertEquals(x, motion[0], EPS);
        assertEquals(y, motion[1], EPS);
        assertEquals(z, motion[2], EPS);
    }

    @Test
    void addsLiftEachTick() {
        Wind u = column(0.1, 0.5);
        assertMotion(0, 0.02, 0, u.push(0, -0.08, 0));
        assertMotion(0, 0.12, 0, u.push(0, 0.02, 0));
    }

    @Test
    void stopsAtTheCap() {
        assertMotion(0, 0.5, 0, column(0.1, 0.5).push(0, 0.45, 0));
    }

    @Test
    void leavesAFasterRiseAlone() {
        // A jump already rising faster than the cap is not slowed down.
        assertMotion(0, 0.8, 0, column(0.1, 0.5).push(0, 0.8, 0));
    }

    @Test
    void pushesSidewaysWithoutTouchingTheFall() {
        assertMotion(0.1, -0.08, 0.3, toward(1, 0, 0, 0.1, 0.5).push(0, -0.08, 0.3));
    }

    @Test
    void pushesAlongATiltedDirectionOfAnyLength() {
        double h = 0.1 / Math.sqrt(2);
        assertMotion(h, 0, -h, toward(2, 0, -2, 0.1, 0.5).push(0, 0, 0));
    }

    @Test
    void capsOnlyTheSpeedAlongTheDirection() {
        // Moving across the gust does not count toward its cap.
        assertMotion(0.5, 0.3, 0.9, toward(1, 0, 0, 0.1, 0.5).push(0.45, 0.3, 0.9));
    }

    @Test
    void slowsWhatMovesAgainstIt() {
        assertMotion(0, 0.1, 0, toward(0, -1, 0, 0.1, 0.5).push(0, 0.2, 0));
    }

    @Test
    void breaksTheFallOnlyWhenPushingUpward() {
        assertTrue(column(0.1, 0.5).breaksFall());
        assertTrue(toward(1, 0.2, 0, 0.1, 0.5).breaksFall());
        assertFalse(toward(1, 0, 0, 0.1, 0.5).breaksFall());
        assertFalse(toward(0, -1, 0, 0.1, 0.5).breaksFall());
    }

    @Test
    void rejectsAnEmptyBoxNoLiftOrNoDirection() {
        assertThrows(IllegalArgumentException.class, () -> new Wind(0, 0, 0, 0, 10, 4, 0.1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> column(0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> column(0.1, 0));
        assertThrows(IllegalArgumentException.class, () -> toward(0, 0, 0, 0.1, 0.5));
    }

    @Test
    void strongestPicksTheHighestCapAmongThoseCatchingTheBox() {
        Wind weak = column(0.2, 0.4);
        Wind strong = column(0.1, 0.9);
        Wind far = new Wind(100, 0, 100, 104, 10, 104, 0.5, 2);
        assertSame(strong, Wind.strongest(Arrays.asList(weak, far, strong), 1, 1, 1, 2, 3, 2));
    }

    @Test
    void strongestBreaksATieOnLift() {
        Wind slow = column(0.1, 0.5);
        Wind quick = column(0.2, 0.5);
        assertSame(quick, Wind.strongest(Arrays.asList(slow, quick), 1, 1, 1, 2, 3, 2));
        assertSame(quick, Wind.strongest(Arrays.asList(quick, slow), 1, 1, 1, 2, 3, 2));
    }

    @Test
    void strongestIsNullOutsideEveryWind() {
        assertNull(Wind.strongest(Collections.singletonList(column(0.1, 0.5)), 50, 0, 50, 51, 2, 51));
    }
}

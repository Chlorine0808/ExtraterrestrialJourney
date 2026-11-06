package chlorine.etjourney.world.updraft.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** An updraft catches a box overlapping it and speeds it upward until the cap. */
class UpdraftTest {

    private static final double EPS = 1e-9;

    private static Updraft column(double lift, double maxSpeed) {
        return new Updraft(0, 0, 0, 4, 10, 4, lift, maxSpeed);
    }

    @Test
    void catchesABoxThatOverlapsIt() {
        Updraft u = column(0.1, 0.5);
        assertTrue(u.overlaps(1, 2, 1, 1.6, 3.8, 1.6));
        // Reaching in from outside at one corner is enough.
        assertTrue(u.overlaps(-0.5, -1, -0.5, 0.1, 0.8, 0.1));
    }

    @Test
    void missesABoxThatOnlyTouchesItsFace() {
        Updraft u = column(0.1, 0.5);
        // Standing on top of the column, or flush against its side, is not inside it.
        assertFalse(u.overlaps(1, 10, 1, 1.6, 11.8, 1.6));
        assertFalse(u.overlaps(4, 2, 1, 4.6, 3.8, 1.6));
    }

    @Test
    void addsLiftEachTick() {
        Updraft u = column(0.1, 0.5);
        assertEquals(0.02, u.push(-0.08), EPS);
        assertEquals(0.12, u.push(0.02), EPS);
    }

    @Test
    void stopsAtTheCap() {
        assertEquals(0.5, column(0.1, 0.5).push(0.45), EPS);
    }

    @Test
    void leavesAFasterRiseAlone() {
        // A jump already rising faster than the cap is not slowed down.
        assertEquals(0.8, column(0.1, 0.5).push(0.8), EPS);
    }

    @Test
    void rejectsAnEmptyBoxOrNoLift() {
        assertThrows(IllegalArgumentException.class, () -> new Updraft(0, 0, 0, 0, 10, 4, 0.1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> column(0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> column(0.1, 0));
    }

    @Test
    void strongestPicksTheHighestCapAmongThoseCatchingTheBox() {
        Updraft weak = column(0.2, 0.4);
        Updraft strong = column(0.1, 0.9);
        Updraft far = new Updraft(100, 0, 100, 104, 10, 104, 0.5, 2);
        assertSame(strong, Updraft.strongest(Arrays.asList(weak, far, strong), 1, 1, 1, 2, 3, 2));
    }

    @Test
    void strongestBreaksATieOnLift() {
        Updraft slow = column(0.1, 0.5);
        Updraft quick = column(0.2, 0.5);
        assertSame(quick, Updraft.strongest(Arrays.asList(slow, quick), 1, 1, 1, 2, 3, 2));
        assertSame(quick, Updraft.strongest(Arrays.asList(quick, slow), 1, 1, 1, 2, 3, 2));
    }

    @Test
    void strongestIsNullOutsideEveryUpdraft() {
        assertNull(Updraft.strongest(Collections.singletonList(column(0.1, 0.5)), 50, 0, 50, 51, 2, 51));
    }
}

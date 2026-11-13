package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An area's radius grows in, shrinks out at the end, wears down with use, and ends with time or size. */
class AreaSizeTest {

    private static final double EPS = 1e-9;

    @Test
    void aPlainAreaKeepsItsRadiusForItsWholeLife() {
        AreaSize size = new AreaSize(2, 2, 0, 0, 0, 0.5, 100);
        assertEquals(2, size.radius(0, 0), EPS);
        assertEquals(2, size.radius(99, 0), EPS);
        assertTrue(size.alive(99, 0));
        assertFalse(size.alive(100, 0));
    }

    @Test
    void itGrowsFromItsStartRadiusOverTheGrowTicks() {
        AreaSize size = new AreaSize(1, 3, 20, 0, 0, 0.5, 100);
        assertEquals(1, size.radius(0, 0), EPS);
        assertEquals(2, size.radius(10, 0), EPS);
        assertEquals(3, size.radius(20, 0), EPS);
        assertEquals(3, size.radius(50, 0), EPS);
    }

    @Test
    void itShrinksToNothingOverItsLastTicks() {
        AreaSize size = new AreaSize(2, 2, 0, 40, 0, 0, 100);
        assertEquals(2, size.radius(60, 0), EPS);
        assertEquals(1, size.radius(80, 0), EPS);
        assertEquals(0.05, size.radius(99, 0), EPS);
    }

    @Test
    void eachUseWearsItDownUntilItIsTooSmallToStay() {
        AreaSize size = new AreaSize(2, 2, 0, 0, 0.25, 0.5, 100);
        assertEquals(1.5, size.radius(0, 2), EPS);
        assertTrue(size.alive(0, 6));
        assertFalse(size.alive(0, 7));
    }

    @Test
    void wearNeverDrivesTheRadiusBelowZero() {
        AreaSize size = new AreaSize(2, 2, 0, 0, 1, 0, 100);
        assertEquals(0, size.radius(0, 5), EPS);
    }
}

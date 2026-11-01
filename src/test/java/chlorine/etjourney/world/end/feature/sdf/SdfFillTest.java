package chlorine.etjourney.world.end.feature.sdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** The flood fill: cells inside the shape, connected to the start, within the bounds. */
class SdfFillTest {

    private static final int[] WIDE = { -20, -20, -20, 20, 20, 20 };

    @Test
    void fillsABallFromItsCentre() {
        Set<Long> cells = SdfFill.fill(Sdf.sphere(2.5), 0, 0, 0, WIDE);
        // Integer points strictly within 2.5 of the origin.
        int expected = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (x * x + y * y + z * z < 6.25) expected++;
                }
            }
        }
        assertEquals(expected, cells.size());
        assertTrue(cells.contains(SdfFill.key(2, 0, 0)));
        assertFalse(cells.contains(SdfFill.key(3, 0, 0)));
    }

    @Test
    void skipsPartsNotConnectedToTheStart() {
        Sdf two = Sdf.sphere(1.5)
            .union(
                Sdf.sphere(1.5)
                    .translate(10, 0, 0));
        Set<Long> cells = SdfFill.fill(two, 0, 0, 0, WIDE);
        assertTrue(cells.contains(SdfFill.key(1, 0, 0)));
        assertFalse(cells.contains(SdfFill.key(10, 0, 0)));
    }

    @Test
    void staysWithinTheBounds() {
        Set<Long> cells = SdfFill.fill(Sdf.sphere(5), 0, 0, 0, new int[] { -2, 0, -20, 20, 20, 1 });
        for (long key : cells) {
            assertTrue(SdfFill.x(key) >= -2);
            assertTrue(SdfFill.y(key) >= 0);
            assertTrue(SdfFill.z(key) <= 1);
        }
        assertTrue(cells.contains(SdfFill.key(-2, 0, 1)));
    }

    @Test
    void emptyWhenTheStartIsOutside() {
        assertTrue(
            SdfFill.fill(
                Sdf.sphere(1)
                    .translate(5, 0, 0),
                0,
                0,
                0,
                WIDE)
                .isEmpty());
    }

    @Test
    void keysRoundTripNegativeCoordinates() {
        long key = SdfFill.key(-37, 5, -1);
        assertEquals(-37, SdfFill.x(key));
        assertEquals(5, SdfFill.y(key));
        assertEquals(-1, SdfFill.z(key));
        assertEquals(SdfFill.key(-37, 6, -1), SdfFill.offset(key, 0, 1, 0));
    }
}

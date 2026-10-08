package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Springs sit just under a rim top and face open void. */
class FallsTest {

    /** Land for x < 8 with its top at Y 80, void beyond. */
    private static final Falls.Ground CLIFF = (x, z) -> x < 8 ? 80 : -1;

    @Test
    void springsSitInTheRimFacingTheVoid() {
        int found = 0;
        for (int attempt = 0; attempt < 200; attempt++) {
            int[] s = Falls.spring(attempt, 0, 0, attempt, CLIFF);
            if (s == null) continue;
            found++;
            assertEquals(7, s[0]);
            assertEquals(77, s[1]);
        }
        assertTrue(found > 0);
    }

    @Test
    void fallsThinOutAcrossTheBorder() {
        int full = 0, half = 0, none = 0;
        for (int cx = 0; cx < 40; cx++) {
            for (int cz = 0; cz < 40; cz++) {
                if (Falls.forms(5L, cx, cz, 1)) full++;
                if (Falls.forms(5L, cx, cz, 0.35)) half++;
                if (Falls.forms(5L, cx, cz, 0.05)) none++;
            }
        }
        assertEquals(1600, full);
        assertEquals(0, none);
        assertTrue(half > full / 5 && half < full * 4 / 5, half + " of " + full);
    }

    @Test
    void noSpringWithoutAnEdge() {
        for (int attempt = 0; attempt < 50; attempt++) assertNull(Falls.spring(1, 0, 0, attempt, (x, z) -> 80));
    }

    @Test
    void noSpringOverAShallowStep() {
        Falls.Ground step = (x, z) -> x < 8 ? 80 : 70;
        for (int attempt = 0; attempt < 200; attempt++) assertNull(Falls.spring(attempt, 0, 0, attempt, step));
    }

    @Test
    void sameInputsGiveTheSameSpring() {
        for (int attempt = 0; attempt < 200; attempt++) {
            int[] a = Falls.spring(5, 0, 0, attempt, CLIFF);
            if (a == null) continue;
            assertNotNull(Falls.spring(5, 0, 0, attempt, CLIFF));
            assertEquals(a[2], Falls.spring(5, 0, 0, attempt, CLIFF)[2]);
        }
    }
}

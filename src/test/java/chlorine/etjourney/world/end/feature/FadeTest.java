package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Placed features thin out across the region border instead of stopping at one weight. */
class FadeTest {

    @Test
    void nothingAtTheOuterEdgeAndEverythingInside() {
        assertEquals(0, Fade.of(0), 0);
        assertEquals(0, Fade.of(0.1), 0);
        assertEquals(1, Fade.of(0.6), 0);
        assertEquals(1, Fade.of(0.8), 0);
        assertEquals(1, Fade.of(1), 0);
    }

    @Test
    void risesSteadilyAcrossTheBorder() {
        double last = 0;
        for (double w = 0.1; w <= 0.6; w += 0.01) {
            double f = Fade.of(w);
            assertTrue(f >= last, "dropped at " + w);
            last = f;
        }
        double mid = Fade.of(0.35);
        assertTrue(mid > 0.3 && mid < 0.7, "middle " + mid);
    }

    @Test
    void formsKeepsRollsUnderTheFade() {
        assertTrue(Fade.forms(1, 0.999));
        assertFalse(Fade.forms(0, 0));
        assertTrue(Fade.forms(0.35, 0.1));
        assertFalse(Fade.forms(0.35, 0.9));
    }
}

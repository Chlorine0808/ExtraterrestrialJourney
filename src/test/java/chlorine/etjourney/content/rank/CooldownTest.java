package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An ability is ready once its length has passed since the last use, or if it was never used. */
class CooldownTest {

    @Test
    void neverUsedIsReady() {
        assertTrue(Cooldown.ready(5, Cooldown.NEVER, 100));
    }

    @Test
    void waitsTheWholeLength() {
        assertFalse(Cooldown.ready(1099, 1000, 100));
        assertTrue(Cooldown.ready(1100, 1000, 100));
    }

    @Test
    void aClockThatWentBackIsReady() {
        assertTrue(Cooldown.ready(10, 1000, 100));
    }
}

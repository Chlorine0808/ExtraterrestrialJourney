package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An area works on a newcomer at once, then again only after its interval, keeping count per creature. */
class PulseClockTest {

    @Test
    void aNewcomerIsWorkedOnAtOnceThenAfterTheInterval() {
        PulseClock clock = new PulseClock(20);
        assertTrue(clock.due(7, 100));
        assertFalse(clock.due(7, 101));
        assertFalse(clock.due(7, 119));
        assertTrue(clock.due(7, 120));
    }

    @Test
    void eachCreatureKeepsItsOwnTime() {
        PulseClock clock = new PulseClock(20);
        assertTrue(clock.due(1, 100));
        assertTrue(clock.due(2, 105));
        assertFalse(clock.due(2, 120));
        assertTrue(clock.due(1, 120));
    }

    @Test
    void steppingOutAndBackDoesNotResetTheTime() {
        PulseClock clock = new PulseClock(20);
        assertTrue(clock.due(1, 100));
        // Nothing marks leaving, so a return at 110 is still inside the interval.
        assertFalse(clock.due(1, 110));
    }
}

package chlorine.etjourney.core.accessory.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CadenceTest {

    @Test
    void isDueAtOnceTheFirstTime() {
        assertTrue(new Cadence(20).due(12345));
    }

    @Test
    void waitsTheWholePeriodBetweenRuns() {
        Cadence c = new Cadence(20);
        c.due(100);
        assertFalse(c.due(119));
        assertTrue(c.due(120));
    }

    @Test
    void manyCallsInOneTickRunOnce() {
        Cadence c = new Cadence(20);
        assertTrue(c.due(100));
        assertFalse(c.due(100));
        assertFalse(c.due(100));
    }

    @Test
    void aClockThatGoesBackRunsAgain() {
        Cadence c = new Cadence(20);
        c.due(5000);
        assertTrue(c.due(10));
    }
}

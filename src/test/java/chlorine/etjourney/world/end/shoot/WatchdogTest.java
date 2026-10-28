package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WatchdogTest {

    @Test
    void firesOnlyAfterTheLimitWithoutProgress() {
        Watchdog w = new Watchdog(1000, 0);
        assertFalse(w.expired(1000));
        assertTrue(w.expired(1001));
    }

    @Test
    void progressRestartsTheClock() {
        Watchdog w = new Watchdog(1000, 0);
        w.progress(900);
        assertFalse(w.expired(1800));
        assertTrue(w.expired(1901));
    }
}

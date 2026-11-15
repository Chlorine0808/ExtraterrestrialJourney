package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A player gets a new grid on first sight, on a new cell, and after changing dimension. */
class CellWatchTest {

    private final CellWatch watch = new CellWatch();
    private final Object player = new Object();

    @Test
    void firstSightSends() {
        assertTrue(watch.moved(player, 1, 0, 0));
    }

    @Test
    void sameCellIsQuiet() {
        watch.moved(player, 1, 3, -4);
        assertFalse(watch.moved(player, 1, 3, -4));
    }

    @Test
    void newCellSends() {
        watch.moved(player, 1, 3, -4);
        assertTrue(watch.moved(player, 1, 3, -5));
    }

    @Test
    void dimensionChangeResends() {
        watch.moved(player, 1, 3, -4);
        assertTrue(watch.moved(player, 0, 3, -4));
        assertTrue(watch.moved(player, 1, 3, -4));
    }

    @Test
    void forgottenPlayerIsNewAgain() {
        watch.moved(player, 1, 3, -4);
        watch.forget(player);
        assertTrue(watch.moved(player, 1, 3, -4));
    }
}

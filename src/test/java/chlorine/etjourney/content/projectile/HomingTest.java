package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A homing shot flies straight at first, turns for a while, then flies straight again. */
class HomingTest {

    @Test
    void turnsOnlyInsideItsWindow() {
        Homing h = new Homing(0.1, 5, 10, 16);
        assertFalse(h.activeAt(4));
        assertTrue(h.activeAt(5));
        assertTrue(h.activeAt(14));
        assertFalse(h.activeAt(15));
    }

    @Test
    void aNegativeDurationTurnsForever() {
        assertTrue(new Homing(0.1, 0, -1, 16).activeAt(100_000));
    }

    @Test
    void rejectsNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new Homing(0, 0, -1, 16));
        assertThrows(IllegalArgumentException.class, () -> new Homing(0.1, -1, -1, 16));
        assertThrows(IllegalArgumentException.class, () -> new Homing(0.1, 0, -1, 0));
    }
}

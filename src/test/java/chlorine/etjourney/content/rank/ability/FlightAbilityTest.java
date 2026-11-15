package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The mob rises only when its target is more than 1.5 blocks above it. */
class FlightAbilityTest {

    @Test
    void risesToAHigherTarget() {
        assertTrue(FlightAbility.rises(64, 66));
    }

    @Test
    void staysForALevelOrLowerTarget() {
        assertFalse(FlightAbility.rises(64, 64));
        assertFalse(FlightAbility.rises(64, 50));
    }

    @Test
    void staysAtTheThreshold() {
        assertFalse(FlightAbility.rises(64, 65.5));
        assertTrue(FlightAbility.rises(64, 65.6));
    }
}

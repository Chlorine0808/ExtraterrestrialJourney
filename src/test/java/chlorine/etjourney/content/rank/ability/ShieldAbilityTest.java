package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shield refills once 160 ticks have passed since the last hit, and only when it is short. */
class ShieldAbilityTest {

    @Test
    void refillsAfterTheWait() {
        assertTrue(ShieldAbility.shouldRefill(1160, 1000, 3f, 10f));
    }

    @Test
    void doesNotRefillBeforeTheWait() {
        assertFalse(ShieldAbility.shouldRefill(1159, 1000, 3f, 10f));
    }

    @Test
    void doesNotRefillAFullShield() {
        assertFalse(ShieldAbility.shouldRefill(5000, 1000, 10f, 10f));
    }

    @Test
    void refillsAShieldNeverHit() {
        assertTrue(ShieldAbility.shouldRefill(5, -1, 0f, 10f));
    }
}

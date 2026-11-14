package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Enrage starts below 40% of the maximum health, not at it. */
class EnrageAbilityTest {

    @Test
    void enragesBelowTheThreshold() {
        assertTrue(EnrageAbility.enraged(39f, 100f));
    }

    @Test
    void doesNotEnrageAtOrAboveIt() {
        assertFalse(EnrageAbility.enraged(40f, 100f));
        assertFalse(EnrageAbility.enraged(100f, 100f));
    }
}

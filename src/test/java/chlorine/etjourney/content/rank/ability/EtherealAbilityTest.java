package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A hit is physical unless it is magic, fire, an explosion, unblockable, or an elemental one with magic or elements.
 */
class EtherealAbilityTest {

    @Test
    void anOrdinaryHitIsPhysical() {
        assertTrue(EtherealAbility.isPhysical(false, false, false, false, false, false));
    }

    @Test
    void magicFireExplosionAndUnblockableAreNot() {
        assertFalse(EtherealAbility.isPhysical(true, false, false, false, false, false));
        assertFalse(EtherealAbility.isPhysical(false, true, false, false, false, false));
        assertFalse(EtherealAbility.isPhysical(false, false, true, false, false, false));
        assertFalse(EtherealAbility.isPhysical(false, false, false, true, false, false));
    }

    @Test
    void anElementalHitIsPhysicalOnlyWhenPlainPhysical() {
        assertTrue(EtherealAbility.isPhysical(false, false, false, false, true, true));
        assertFalse(EtherealAbility.isPhysical(false, false, false, false, true, false));
    }
}

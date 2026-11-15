package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The guard covers 60 degrees to each side of the body yaw; yaw 0 faces +z, yaw 90 faces -x. */
class GuardAbilityTest {

    @Test
    void facingTheSourceIsInFront() {
        assertTrue(GuardAbility.inFront(0, 0, 5));
        assertTrue(GuardAbility.inFront(90, -5, 0));
        assertTrue(GuardAbility.inFront(180, 0, -5));
        assertTrue(GuardAbility.inFront(-90, 5, 0));
    }

    @Test
    void behindIsNotInFront() {
        assertFalse(GuardAbility.inFront(0, 0, -5));
        assertFalse(GuardAbility.inFront(90, 5, 0));
    }

    @Test
    void theSideIsNotInFront() {
        assertFalse(GuardAbility.inFront(0, 5, 0));
        assertFalse(GuardAbility.inFront(0, -5, 0));
    }

    @Test
    void theEdgeIsInsideAndJustPastItIsOut() {
        assertTrue(GuardAbility.inFront(0, -Math.sin(Math.toRadians(60)), Math.cos(Math.toRadians(60))));
        assertTrue(GuardAbility.inFront(0, -Math.sin(Math.toRadians(59)), Math.cos(Math.toRadians(59))));
        assertFalse(GuardAbility.inFront(0, -Math.sin(Math.toRadians(61)), Math.cos(Math.toRadians(61))));
        assertTrue(GuardAbility.inFront(0, Math.sin(Math.toRadians(60)), Math.cos(Math.toRadians(60))));
        assertFalse(GuardAbility.inFront(0, Math.sin(Math.toRadians(61)), Math.cos(Math.toRadians(61))));
    }

    @Test
    void yawWrapsAroundHalfTurn() {
        // Yaw 170 and a source at -170 are 20 degrees apart across the wrap.
        double rad = Math.toRadians(-170);
        assertTrue(GuardAbility.inFront(170, -Math.sin(rad), Math.cos(rad)));
        // The same with an unwrapped yaw.
        assertTrue(GuardAbility.inFront(530, -Math.sin(rad), Math.cos(rad)));
        assertTrue(GuardAbility.inFront(-190, -Math.sin(rad), Math.cos(rad)));
    }

    @Test
    void aSourceOnTheMobIsNotInFront() {
        assertFalse(GuardAbility.inFront(0, 0, 0));
    }
}

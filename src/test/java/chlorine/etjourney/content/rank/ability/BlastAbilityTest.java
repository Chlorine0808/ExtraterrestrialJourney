package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The blast hurts most at its centre, less the farther out, and not at all from the radius on. */
class BlastAbilityTest {

    @Test
    void isFullAtTheCentre() {
        assertEquals(BlastAbility.MAX, BlastAbility.damage(0), 1e-6);
    }

    @Test
    void fallsOffInAStraightLine() {
        assertEquals(BlastAbility.MAX / 2, BlastAbility.damage(BlastAbility.RADIUS / 2), 1e-6);
        assertTrue(BlastAbility.damage(1) > BlastAbility.damage(3));
    }

    @Test
    void isNothingAtAndBeyondTheRadius() {
        assertEquals(0, BlastAbility.damage(BlastAbility.RADIUS), 1e-6);
        assertEquals(0, BlastAbility.damage(BlastAbility.RADIUS + 5), 1e-6);
    }
}

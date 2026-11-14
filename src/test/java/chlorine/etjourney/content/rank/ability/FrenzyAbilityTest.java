package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The extra hit needs the target within the vanilla melee reach: (2 * width) squared plus the target's width. */
class FrenzyAbilityTest {

    @Test
    void reachesJustInside() {
        // width 0.6 and 0.6: reach squared is 1.44 + 0.6
        assertTrue(FrenzyAbility.inReach(2.03, 0.6f, 0.6f));
    }

    @Test
    void doesNotReachBeyondIt() {
        assertFalse(FrenzyAbility.inReach(2.2, 0.6f, 0.6f));
    }

    @Test
    void aWiderTargetIsReachedFromFurther() {
        assertFalse(FrenzyAbility.inReach(3.0, 0.6f, 0.6f));
        assertTrue(FrenzyAbility.inReach(3.0, 0.6f, 1.8f));
    }
}

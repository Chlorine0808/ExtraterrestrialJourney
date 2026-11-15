package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A mark holds up to, but not at, the tick it runs out. */
class FamineAbilityTest {

    @Test
    void holdsBeforeItRunsOut() {
        assertTrue(FamineAbility.marked(100, 130));
        assertTrue(FamineAbility.marked(129, 130));
    }

    @Test
    void endsAtTheUntilTick() {
        assertFalse(FamineAbility.marked(130, 130));
        assertFalse(FamineAbility.marked(500, 130));
    }

    @Test
    void anUnsetMarkIsNotOne() {
        assertFalse(FamineAbility.marked(100, 0));
    }
}

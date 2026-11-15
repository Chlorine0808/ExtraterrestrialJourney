package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Regen heals 2% of the maximum health, at least 1. */
class RegenAbilityTest {

    @Test
    void healsTwoPercentOfALargeMaximum() {
        assertEquals(4f, RegenAbility.amount(200f), 1e-4);
    }

    @Test
    void healsOneAtLeast() {
        assertEquals(1f, RegenAbility.amount(20f), 1e-4);
    }
}

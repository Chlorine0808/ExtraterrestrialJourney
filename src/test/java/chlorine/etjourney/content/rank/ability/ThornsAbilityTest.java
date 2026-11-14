package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Thorns return 30% of the hit, and at least one point. */
class ThornsAbilityTest {

    @Test
    void returnsThirtyPercent() {
        assertEquals(6f, ThornsAbility.reflected(20f), 1e-4);
    }

    @Test
    void returnsAtLeastOne() {
        assertEquals(1f, ThornsAbility.reflected(2f), 1e-4);
        assertEquals(1f, ThornsAbility.reflected(0.1f), 1e-4);
    }
}

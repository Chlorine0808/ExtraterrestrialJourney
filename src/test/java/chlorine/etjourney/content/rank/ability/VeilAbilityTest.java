package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Neutral hits are doubled and every other element is cut to a quarter. */
class VeilAbilityTest {

    @Test
    void neutralIsDoubled() {
        assertEquals(10f, VeilAbility.scaled(true, 5f), 1e-4);
    }

    @Test
    void otherElementsAreCutToAQuarter() {
        assertEquals(1.25f, VeilAbility.scaled(false, 5f), 1e-4);
    }
}

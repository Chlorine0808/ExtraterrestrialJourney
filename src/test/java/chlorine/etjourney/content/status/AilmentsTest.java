package chlorine.etjourney.content.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AilmentsTest {

    @Test
    void eachLevelOfHeavyTakesAQuarterUntilNothingIsLeft() {
        assertEquals(0.75f, Ailments.jumpLeft(0), 1e-6);
        assertEquals(0.5f, Ailments.jumpLeft(1), 1e-6);
        assertEquals(0f, Ailments.jumpLeft(3), 1e-6);
        assertEquals(0f, Ailments.jumpLeft(9), 1e-6);
    }
}

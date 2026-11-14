package chlorine.etjourney.core.status.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PotionSlotsTest {

    @Test
    void aFreePreferredIdIsKept() {
        Object[] table = new Object[32];
        assertEquals(28, PotionSlots.claim(table, 28, 24));
    }

    @Test
    void aTakenOrOutsideIdFallsToTheFirstFree() {
        Object[] table = new Object[32];
        table[28] = "taken";
        table[24] = "taken";
        assertEquals(25, PotionSlots.claim(table, 28, 24));
        assertEquals(25, PotionSlots.claim(table, 40, 24));
        assertEquals(25, PotionSlots.claim(table, 3, 24));
    }

    @Test
    void aFullTableHasNoId() {
        Object[] table = new Object[26];
        table[24] = "taken";
        table[25] = "taken";
        assertEquals(-1, PotionSlots.claim(table, 24, 24));
    }
}

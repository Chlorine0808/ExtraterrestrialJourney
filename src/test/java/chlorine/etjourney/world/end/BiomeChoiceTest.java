package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A pick in [0, 1) selects one of the biomes, each over an equal share. */
class BiomeChoiceTest {

    @Test
    void picksSplitTheListEvenly() {
        assertEquals(0, BiomeChoice.index(4, 0));
        assertEquals(0, BiomeChoice.index(4, 0.24));
        assertEquals(1, BiomeChoice.index(4, 0.25));
        assertEquals(3, BiomeChoice.index(4, 0.999999));
    }

    @Test
    void outOfRangePicksStayInTheList() {
        assertEquals(3, BiomeChoice.index(4, 1));
        assertEquals(0, BiomeChoice.index(4, -0.5));
    }

    @Test
    void anEmptyListHasNoIndex() {
        assertEquals(-1, BiomeChoice.index(0, 0.5));
    }
}

package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LandTest {

    @Test
    void voidBasesRemoveTheContinent() {
        for (int x = 2000; x < 4000; x += 37) {
            assertTrue(Land.height(9L, x, 500, 0, 1) < 0);
            assertTrue(Land.height(9L, x, 500, 0, 0) <= Continent.MAX_HEIGHT);
        }
    }
}

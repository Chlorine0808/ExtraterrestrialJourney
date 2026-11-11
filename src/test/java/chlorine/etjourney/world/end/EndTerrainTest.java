package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EndTerrainTest {

    @Test
    void blockColumnIsInTerrainFromItsCentreOutward() {
        // Column 399 is centred at 399.5, column 400 at 400.5.
        assertFalse(EndTerrain.inTerrain(399, 0));
        assertTrue(EndTerrain.inTerrain(400, 0));
        assertFalse(EndTerrain.inTerrain(-400, 0));
        assertTrue(EndTerrain.inTerrain(-401, 0));
        assertTrue(EndTerrain.inTerrain(300, 300));
        assertFalse(EndTerrain.inTerrain(0, 0));
    }
}

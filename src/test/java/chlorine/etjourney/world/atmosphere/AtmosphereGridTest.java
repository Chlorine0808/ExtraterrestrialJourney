package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The grid sits on 32-block points centred on the player's cell, and answers outside itself with its edge. */
class AtmosphereGridTest {

    @Test
    void negativeCoordinatesFloor() {
        assertEquals(-1, AtmosphereGrid.cellOf(-1));
        assertEquals(-1, AtmosphereGrid.cellOf(-32));
        assertEquals(-2, AtmosphereGrid.cellOf(-32.5));
        assertEquals(0, AtmosphereGrid.cellOf(31.9));
    }

    @Test
    void aroundCentresOnTheCell() {
        List<int[]> asked = new ArrayList<>();
        AtmosphereGrid g = AtmosphereGrid.around(70, -10, (x, z) -> {
            asked.add(new int[] { x, z });
            return (byte) (x / 32 + 10);
        });
        assertEquals(0, g.originX());
        assertEquals(-3, g.originZ());
        assertEquals(AtmosphereGrid.SIZE * AtmosphereGrid.SIZE, asked.size());
        assertEquals(0, asked.get(0)[0]);
        assertEquals(-96, asked.get(0)[1]);
        assertEquals(10 + 4, g.idAt(4, 0));
    }

    @Test
    void outsideClampsToEdge() {
        byte[] ids = new byte[AtmosphereGrid.SIZE * AtmosphereGrid.SIZE];
        ids[0] = 3;
        ids[ids.length - 1] = 5;
        AtmosphereGrid g = new AtmosphereGrid(10, 20, ids);
        assertEquals(3, g.idAt(-100, -100));
        assertEquals(5, g.idAt(100, 100));
    }
}

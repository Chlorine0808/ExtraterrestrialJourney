package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.Layer;

/** The cloud sea is one level sheet with holes, pierced by the peaks that reach it. */
class CloudSeaTest {

    private static List<Layer> at(double x, double z, double ground, double weight) {
        List<Layer> out = new ArrayList<>();
        CloudSea.sheets(63L, x, z, ground, weight, out);
        return out;
    }

    @Test
    void nothingAtWeightZero() {
        for (int i = 0; i < 1000; i++) assertEquals(0, at(5000 + i * 3, 0, 40, 0).size());
    }

    @Test
    void theSheetIsLevelAndThreeToEightBlocksThick() {
        int sheets = 0, columns = 3000;
        for (int i = 0; i < columns; i++) {
            List<Layer> column = at(5000 + (i % 60) * 4, (i / 60) * 4, 40, 1);
            for (Layer l : column) {
                double blocks = l.top - l.bottom + 1;
                assertTrue(blocks >= 3 && blocks <= 8, "thickness " + blocks);
                assertTrue(l.top >= 95 && l.top <= 107, "top " + l.top);
                sheets++;
            }
        }
        // Mostly covered, with holes onto the void.
        assertTrue(sheets > columns / 2 && sheets < columns * 95 / 100, sheets + " of " + columns);
    }

    @Test
    void peaksPierceTheSea() {
        for (int i = 0; i < 1000; i++) {
            double x = 5000 + i * 3, z = 0;
            assertEquals(0, at(x, z, CloudSea.level(63L, x, z), 1).size());
        }
    }
}

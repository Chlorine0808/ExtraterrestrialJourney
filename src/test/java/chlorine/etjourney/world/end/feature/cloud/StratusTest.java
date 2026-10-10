package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.Layer;

/** STRATUS stacks thin sheets over the ground and leaves nothing where its weight is gone. */
class StratusTest {

    private static List<Layer> at(double x, double z, double weight) {
        List<Layer> out = new ArrayList<>();
        Stratus.sheets(41L, x, z, 70, weight, out);
        return out;
    }

    @Test
    void nothingAtWeightZero() {
        for (int i = 0; i < 500; i++) assertEquals(0, at(3000 + i * 7, 2000, 0).size());
    }

    @Test
    void sheetsAreOneToFourBlocksThickAndBelowTheTop() {
        int sheets = 0;
        for (int i = 0; i < 1000; i++) {
            List<Layer> column = at(3000 + i * 7, 2000 + i * 3, 1);
            assertTrue(column.size() <= 12, "too many sheets: " + column.size());
            for (Layer l : column) {
                double blocks = l.top - l.bottom + 1;
                assertTrue(blocks >= 1 && blocks <= 4, "thickness " + blocks);
                assertTrue(l.top <= Stratus.TOP, "top " + l.top);
                assertEquals(Math.floor(l.bottom), l.bottom);
            }
            sheets += column.size();
        }
        // Holes take part of every sheet, but a full-weight column keeps several.
        assertTrue(sheets > 3000, "only " + sheets + " sheets over 1000 columns");
    }

    @Test
    void upperSheetsAreSparser() {
        int low = 0, high = 0;
        for (int i = 0; i < 2000; i++) {
            for (Layer l : at(3000 + i * 5, 2000 + i * 11, 1)) {
                if (l.bottom < 70 + 30) low++;
                else if (l.bottom > 70 + 60) high++;
            }
        }
        assertTrue(low > high, low + " low against " + high + " high");
    }
}

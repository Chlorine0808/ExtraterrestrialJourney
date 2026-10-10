package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.Layer;

/** CIRRUS draws high streaks one or two blocks thick over part of the sky, along the wind of their region. */
class CirrusTest {

    private static List<Layer> at(double x, double z, double weight, double ground) {
        List<Layer> out = new ArrayList<>();
        Cirrus.sheets(52L, x, z, ground, weight, out);
        return out;
    }

    @Test
    void nothingAtWeightZero() {
        for (int i = 0; i < 2000; i++) assertEquals(0, at(4000 + i * 3, 1000 + i * 2, 0, -1000).size());
    }

    @Test
    void streaksAreHighAndThin() {
        int streaks = 0, columns = 4000;
        for (int i = 0; i < columns; i++) {
            List<Layer> column = at(4000 + (i % 64) * 3, 1000 + (i / 64) * 3, 1, -1000);
            assertTrue(column.size() <= 1);
            for (Layer l : column) {
                double blocks = l.top - l.bottom + 1;
                assertTrue(blocks >= 1 && blocks <= 2, "thickness " + blocks);
                assertTrue(l.bottom >= 200 && l.top <= 241, "height " + l.bottom + ".." + l.top);
                streaks++;
            }
        }
        // Streaks, not a ceiling: some of the sky, far from all of it.
        assertTrue(streaks > columns / 20 && streaks < columns * 35 / 100, streaks + " of " + columns);
    }

    @Test
    void streaksHoldFarFromTheOrigin() {
        int streaks = 0, columns = 4000;
        for (int i = 0; i < columns; i++) {
            streaks += at(200000 + (i % 64) * 3, -150000 + (i / 64) * 3, 1, -1000).size();
        }
        assertTrue(streaks > columns / 20 && streaks < columns * 35 / 100, streaks + " of " + columns);
    }

    @Test
    void regionsHaveTheirOwnWinds() {
        double first = Cirrus.wind(52L, 0, 0);
        boolean differs = false;
        for (int i = 1; i < 20; i++) differs |= Math.abs(Cirrus.wind(52L, i, 0) - first) > 0.3;
        assertTrue(differs);
    }

    @Test
    void mountainsReachingTheStreaksPushThemAway() {
        for (int i = 0; i < 2000; i++) assertEquals(0, at(4000 + i * 3, 1000 + i * 2, 1, 245).size());
    }
}

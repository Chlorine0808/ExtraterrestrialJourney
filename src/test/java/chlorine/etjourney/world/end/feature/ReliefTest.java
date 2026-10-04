package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ReliefTest {

    @Test
    void termsStayInTheirRanges() {
        Random r = new Random(3);
        for (int i = 0; i < 2000; i++) {
            double x = r.nextInt(20000), z = r.nextInt(20000);
            double level = Relief.level(4L, x, z);
            assertTrue(level >= 32 && level <= 104, "level " + level);
            assertTrue(Math.abs(Relief.hills(4L, x, z, x, z)) <= 19);
            double ridges = Relief.ridges(4L, x, z);
            assertTrue(ridges >= 0 && ridges <= 1);
            double depth = Valleys.depth(4L, x, z);
            assertTrue(depth >= 0 && depth <= 50);
        }
    }

    @Test
    void valleysExistSomewhere() {
        double deepest = 0;
        for (int x = 0; x < 6000; x += 4) deepest = Math.max(deepest, Valleys.depth(4L, x, 1234));
        assertTrue(deepest > 5, "deepest " + deepest);
    }

    @Test
    void shiftKeepsTheSlabInsideTheWorld() {
        Random r = new Random(4);
        for (int i = 0; i < 10000; i++) {
            double bottom = -80 + r.nextDouble() * 280;
            double shifted = bottom + Underside.shift(bottom);
            assertTrue(shifted >= Underside.MIN_BOTTOM - 1e-9 && shifted <= Underside.MAX_BOTTOM + 1e-9);
        }
        assertEquals(0, Underside.shift(50), 1e-9);
    }
}

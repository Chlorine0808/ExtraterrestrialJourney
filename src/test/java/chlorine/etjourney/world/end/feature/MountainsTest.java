package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class MountainsTest {

    @Test
    void mountainsStandOnLandWithTallOnesRare() {
        int total = 0, tall = 0;
        for (int cx = 4; cx < 40; cx++) {
            for (int cz = -20; cz < 20; cz++) {
                Mountains.Mountain m = Mountains.inCell(5L, cx, cz);
                if (m == null) continue;
                total++;
                if (m.peak > 100) tall++;
                assertTrue(m.peak >= 18 && m.peak <= 168);
                assertTrue(Continent.rawHeight(Continent.seedsNear(5L, m.x, m.z, 0), m.x, m.z) >= 20);
            }
        }
        assertTrue(total > 50, "only " + total);
        double share = tall / (double) total;
        assertTrue(share > 0.08 && share < 0.3, "tall share " + share);
    }

    @Test
    void riseEndsAtTheFoot() {
        Mountains.Mountain m = new Mountains.Mountain(0, 0, 100);
        assertEquals(100, Mountains.rise(Collections.singletonList(m), 0, 0), 1e-9);
        assertEquals(0, Mountains.rise(Collections.singletonList(m), m.radius + 1, 0), 1e-9);
    }
}

package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ContinentTest {

    @Test
    void noSeedNearTheCentreAndHeightIsCapped() {
        for (Continent.Seed s : Continent.seedsNear(11L, 0, 0, 1100)) {
            assertTrue(Math.hypot(s.x, s.z) > 1024);
        }
        List<Continent.Seed> seeds = Continent.seedsNear(11L, 3000, 0, 200);
        for (int x = 2800; x < 3200; x += 7) assertTrue(Continent.rawHeight(seeds, x, 0) <= Continent.MAX_HEIGHT);
    }

    @Test
    void localSearchMatchesAWideSearch() {
        // A seed reaches 100 / 14 * 8 = 57 blocks; the local search must find every seed that matters.
        List<Continent.Seed> wide = Continent.seedsNear(11L, 3000, 3000, 400);
        for (int x = 2800; x < 3200; x += 13) {
            for (int z = 2800; z < 3200; z += 13) {
                assertEquals(
                    Continent.rawHeight(wide, x, z),
                    Continent.rawHeight(Continent.seedsNear(11L, x, z, 0), x, z),
                    1e-9);
            }
        }
    }
}

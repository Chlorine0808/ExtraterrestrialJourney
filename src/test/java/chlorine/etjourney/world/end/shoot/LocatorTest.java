package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Locator on the screenshot seed: what it finds really stands where it says. */
class LocatorTest {

    private static final long SEED = 7037748451318983361L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final Locator LOCATOR = new Locator(SAMPLER);

    @Test
    void continentsOfEachBiomeAreDistinctAndWearIt() {
        for (EndBiome b : EndBiome.values()) {
            List<Target> found = LOCATOR.continents(b, 3000, 0, 2);
            assertEquals(2, found.size(), b.name());
            assertNotEquals(found.get(0).key, found.get(1).key, b.name());
            for (Target t : found) {
                assertEquals(b, SAMPLER.biome(t.x, t.z));
                assertTrue(SAMPLER.land(t.x, t.z) > 10);
            }
        }
    }

    @Test
    void bareContinentHasNoBiome() {
        Target t = LOCATOR.continents(null, 3000, 0, 1)
            .get(0);
        assertNull(SAMPLER.biome(t.x, t.z));
        assertTrue(SAMPLER.land(t.x, t.z) > 10);
    }

    @Test
    void mountainTopReachesTheMinimum() {
        assertTrue(
            LOCATOR.mountains(150, 3000, 0, 1)
                .get(0).y >= 150);
    }

    @Test
    void arcTouchLiesOnTheGround() {
        Target t = LOCATOR.arcTouches(3000, 0, 1)
            .get(0);
        double[] span = SAMPLER.arcLand()
            .span(t.x, t.z);
        assertNotNull(span);
        assertTrue(t.y + t.size >= span[0] && t.y - t.size <= span[1]); // size is the tube radius
    }

    @Test
    void biosphereCandidatesAreRingOrderedDistinctAndCapped() {
        List<Biosphere> balls = LOCATOR.biosphereCandidates(3000, 0, 30);
        assertEquals(30, balls.size());
        int cell = Biosphere.KIND.cell;
        int ocx = (int) Math.floor(3000.0 / cell), ocz = 0;
        int last = 0;
        Set<Long> keys = new HashSet<>();
        for (Biosphere b : balls) {
            int ring = Math.max(
                Math.abs((int) Math.floor(b.centreX / cell) - ocx),
                Math.abs((int) Math.floor(b.centreZ / cell) - ocz));
            assertTrue(ring >= last);
            last = ring;
            assertTrue(keys.add(Locator.ofBall(b).key));
        }
    }

    @Test
    void impossibleTargetsComeBackShort() {
        assertTrue(
            LOCATOR.mountains(400, 3000, 0, 1)
                .isEmpty());
    }
}

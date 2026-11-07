package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;

/** Locator on the screenshot seed: what it finds really stands where it says. */
class LocatorTest {

    private static final long SEED = 7037748451318983361L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final Locator LOCATOR = new Locator(SAMPLER, (cx, cz) -> Collections.emptyList());

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
    void lakesAreDistinctAndStandOnTheLiquid() {
        List<Target> found = LOCATOR.lakes(3000, 0, 3);
        assertEquals(3, found.size());
        Set<Long> keys = new HashSet<>();
        for (Target t : found) {
            assertTrue(keys.add(t.key));
            assertTrue(
                Lakes.near(SEED, t.x, t.z, 1, SAMPLER.lakeProbe())
                    .stream()
                    .anyMatch(l -> l.x == t.x && l.z == t.z && l.waterLevel == t.y));
        }
    }

    @Test
    void lakesDroppedByAReservationAreSkipped() {
        Target nearest = LOCATOR.lakes(3000, 0, 1)
            .get(0);
        Area island = new Area("hee", nearest.x + 150, nearest.z, 128);
        Locator reserved = new Locator(SAMPLER, (cx, cz) -> Collections.singletonList(island));
        for (Target t : reserved.lakes(3000, 0, 3)) assertNotEquals(nearest.key, t.key);
    }

    @Test
    void arcTouchLiesOnTheGround() {
        Target t = LOCATOR.arcTouches(3000, 0, 1)
            .get(0);
        double[] span = SAMPLER.arcLand()
            .span(t.x, t.z);
        assertNotNull(span);
        double tube = 12; // the widest tube radius
        assertTrue(t.y + tube >= span[0] && t.y - tube <= span[1]);
    }

    @Test
    void arcTouchIsFramedWideEnoughToShowWhereTheTubeLands() {
        // Framed by the tube alone, the aerial camera stood 48 blocks off, inside the tubes and the ground they
        // reach (20261029-034306, 36: buried twice and blocked once).
        Target t = LOCATOR.arcTouches(3000, 0, 1)
            .get(0);
        assertTrue(Framing.subjectDistance(t) >= 150);
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

    @Test
    void styledContinentsStandInThatStyle() {
        List<Target> found = LOCATOR.continents(EndBiome.GROVE, "LAYERED", 3000, 0, 2);
        assertEquals(2, found.size());
        assertNotEquals(found.get(0).key, found.get(1).key);
        for (Target t : found) {
            assertEquals(EndBiome.GROVE, SAMPLER.biome(t.x, t.z));
            assertTrue(SAMPLER.styleWeight("LAYERED", t.x, t.z) >= 0.6);
        }
    }

    @Test
    void styledContinentsStandOnTheTopmostSlab() {
        for (Target t : LOCATOR.continents(EndBiome.GROVE, "LAYERED", 3000, 0, 2)) assertEquals(
            SAMPLER.bareColumn(t.x, t.z, true)
                .surface(),
            t.y);
    }

    @Test
    void amberTreesGrowOnLayeredSlabs() {
        List<Target> found = LOCATOR.amberTrees("LAYERED", 3000, 0, 3);
        assertEquals(3, found.size());
        Set<String> spots = new HashSet<>();
        boolean onSlab = false;
        for (Target t : found) {
            assertTrue(spots.add(t.x + "," + t.z), "tree at " + t.x + "," + t.z + " twice");
            assertTrue(SAMPLER.styleWeight("LAYERED", t.x, t.z) >= 0.6);
            ColumnState c = SAMPLER.bareColumn(t.x + 0.5, t.z + 0.5, true);
            double top = Math.floor(c.surface()) + 1;
            assertTrue(t.y <= top && t.y >= top - 6, "root " + t.y + " off the topmost surface " + top);
            onSlab |= c.surface() > c.top + 2;
        }
        assertTrue(onSlab);
    }

    @Test
    void giantTreesStandOnGroveRootsOneEach() {
        List<Target> found = LOCATOR.giantTrees(3000, 0, 3);
        assertEquals(3, found.size());
        Set<Long> keys = new HashSet<>();
        for (Target t : found) {
            assertTrue(keys.add(t.key));
            assertEquals(EndBiome.GROVE, SAMPLER.biome(t.x, t.z));
            assertEquals("巨木", t.label);
            assertEquals(80, t.size, 0);
        }
    }
}

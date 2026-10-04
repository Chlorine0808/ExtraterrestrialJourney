package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Style;
import chlorine.etjourney.world.end.region.StyleWeights;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Defects found by the whole-branch review, each pinned by a test. */
class ReviewFixesTest {

    @Test
    void columnCacheKeysDoNotCollide() {
        // (-3080, -3200) and (-3072, -3192) shared a cache key in chunk (-193, -200).
        TerrainSampler sampler = new TerrainSampler(55L, new RegionPicker(Styles.all()));
        ChunkPlan plan = new ChunkPlan(sampler, -193, -200, (x, z) -> Collections.emptyList());
        plan.column(-3080, -3200);
        assertEquals(-3072, plan.column(-3072, -3192).x, 0);
        assertEquals(-3192, plan.column(-3072, -3192).z, 0);
    }

    @Test
    void styleWeightsDoNotDependOnQueryOrderWithinABlock() {
        RegionPicker picker = new RegionPicker(Styles.all());
        TerrainSampler sampler = new TerrainSampler(56L, picker);
        double x = 5000.9, z = -3000.9;
        sampler.weights(5000.1, -3000.1);
        Map<Style, Double> cached = sampler.weights(x, z)
            .asMap();
        Map<Style, Double> fresh = StyleWeights.at(picker, 56L, x, z)
            .asMap();
        assertEquals(fresh, cached);
    }

    @Test
    void holesAreNotPlacedUnderGroundAboveTheGeneratorCeiling() {
        Holes.Probe high = new Holes.Probe() {

            @Override
            public boolean allowed(double x, double z) {
                return true;
            }

            @Override
            public double land(double x, double z) {
                return 80;
            }

            @Override
            public double top(double x, double z) {
                return 150;
            }

            @Override
            public List<Lakes.Lake> lakesNear(double x, double z, double range) {
                return Collections.emptyList();
            }
        };
        assertTrue(
            Holes.near(57L, 6000, 6000, 400, high)
                .isEmpty());
    }

    @Test
    void ourLandIsFullySuppressedInsideTheHeeFootprint() {
        List<Area> area = Collections.singletonList(new Area("hee", 3000, 3000, 128));
        for (int a = 0; a < 360; a += 3) {
            double d = 128 * 1.2 - 0.5;
            double x = 3000 + Math.cos(Math.toRadians(a)) * d, z = 3000 + Math.sin(Math.toRadians(a)) * d;
            assertEquals(1, Reservations.suppression(area, x, z), 1e-9, "angle " + a);
        }
    }
}

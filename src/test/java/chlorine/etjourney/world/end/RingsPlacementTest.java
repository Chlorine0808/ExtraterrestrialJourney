package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Every RINGS region has a ring: anywhere deep inside one, a ring is close enough to see. */
class RingsPlacementTest {

    @Test
    void everyRingsRegionHasARing() {
        long seed = 77L;
        TerrainSampler sampler = new TerrainSampler(seed, new RegionPicker(Styles.all()));
        int samples = 0, near = 0;
        for (int i = 0; i < 160; i++) {
            for (int j = 0; j < 160; j++) {
                double x = -40000 + i * 500, z = -40000 + j * 500;
                if (Math.hypot(x, z) < 2500 || sampler.styleWeight("RINGS", x, z) < 0.75) continue;
                samples++;
                if (!Structures.RINGS.near(seed, x, z, 420, sampler.structureProbe())
                    .isEmpty()) near++;
            }
        }
        assertTrue(samples > 100, "only " + samples + " samples");
        assertTrue(near >= samples * 0.95, near + " of " + samples + " samples deep in RINGS see a ring");
    }
}

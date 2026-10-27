package chlorine.etjourney.world.end.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Mountains;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

class BiomeSearchTest {

    private static final long SEED = 91L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static EndBiome biomeAt(double x, double z) {
        return EndBiome.at(
            SEED,
            Continent.seedsNear(SEED, x, z, Continent.warpReach()),
            Mountains.near(SEED, x, z, 0),
            x,
            z,
            SAMPLER.weights(x, z)
                .voidShare());
    }

    /** Every biome is found on land that really wears it, not far off. */
    @Test
    void findsLandWearingEachBiome() {
        for (EndBiome target : EndBiome.values()) {
            double[] hit = EndSearch.nearestBiome(SAMPLER, target, 3000, 0);
            assertNotNull(hit, "no " + target);
            assertEquals(target, biomeAt(hit[0], hit[1]), "at " + hit[0] + "," + hit[1]);
            assertTrue(SAMPLER.land(hit[0], hit[1]) > 10, "off land at " + hit[0] + "," + hit[1]);
            assertTrue(Math.hypot(hit[0] - 3000, hit[1]) < 3000, target + " far away at " + hit[0] + "," + hit[1]);
        }
    }
}

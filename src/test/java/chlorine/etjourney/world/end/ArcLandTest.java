package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.EndBiome;

/** Arcs laid over painted continents touch the ground and take its biome; others stay bare. */
class ArcLandTest {

    private static final TerrainSampler SAMPLER = ZoneIslandPaintTest.SAMPLER;

    @Test
    void arcsOverPaintedLandTakeItsBiome() {
        int painted = 0, bare = 0;
        for (int i = -60; i < 60 && (painted < 5 || bare < 5); i++) {
            for (int j = -60; j < 60 && (painted < 5 || bare < 5); j++) {
                for (ArcPaths.Path path : ArcPaths.pathsInCell(SAMPLER.seed(), i, j, SAMPLER.arcProbe())) {
                    EndBiome biome = ArcPaths.groundBiome(path, SAMPLER.arcLand());
                    if (biome == null) {
                        bare++;
                        continue;
                    }
                    painted++;
                }
            }
        }
        assertTrue(painted >= 5, "only " + painted + " painted arcs");
        assertTrue(bare >= 5, "only " + bare + " bare arcs");
    }

    @Test
    void groundSpanIsTheLandColumn() {
        ArcPaths.Ground land = SAMPLER.arcLand();
        int checked = 0;
        for (int k = 0; k < 400 && checked < 20; k++) {
            double x = 1500 + k * 37, z = -800 + k * 13;
            double[] span = land.span(x, z);
            if (SAMPLER.land(x, z) <= 0) {
                assertTrue(span == null, "span over the void at " + x + "," + z);
                continue;
            }
            checked++;
            assertTrue(span[1] > span[0], "empty span at " + x + "," + z);
            assertTrue(land.biome(x, z) == SAMPLER.biome(x, z));
        }
        assertTrue(checked >= 20, "only " + checked + " land columns");
    }
}

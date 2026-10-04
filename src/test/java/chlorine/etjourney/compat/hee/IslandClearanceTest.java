package chlorine.etjourney.compat.hee;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

class IslandClearanceTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(41L, new RegionPicker(Styles.all()));

    @Test
    void spheresStayOffLandAndFormOverOpenVoid() {
        boolean sawLand = false, sawVoid = false;
        for (int x = 1200; x < 12000 && !(sawLand && sawVoid); x += 16) {
            double land = SAMPLER.land(x, 0);
            if (land > 20) {
                assertTrue(IslandClearance.blocked(SAMPLER, x, 64, 0));
                sawLand = true;
            } else if (land < -200 && !IslandClearance.blocked(SAMPLER, x, 64, 0)) {
                sawVoid = true;
            }
        }
        assertTrue(sawLand, "no land sampled");
        assertTrue(sawVoid, "no open void sampled");
    }
}

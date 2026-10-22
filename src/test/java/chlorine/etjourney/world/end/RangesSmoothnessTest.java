package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** RANGES crests run in long smooth lines rather than a ragged saw of small spikes. */
class RangesSmoothnessTest {

    private static final long SEED = 91L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    /** Mean of |second difference| of the mountain rise along 1-block steps, over lines lying wholly in RANGES. */
    static double roughness() {
        double sum = 0;
        int n = 0;
        for (int i = 0; i < 400000 && n < 20000; i++) {
            double x = 1500 + (i % 400) * 97, z = 1500 + (i / 400) * 89;
            if (SAMPLER.styleWeight("RANGES", x, z) < 0.99 || SAMPLER.styleWeight("RANGES", x + 64, z) < 0.99) continue;
            double[] top = new double[65];
            boolean land = true;
            for (int k = 0; k <= 64 && land; k++) {
                top[k] = SAMPLER.bareColumn(x + k, z, true).rise;
                land = SAMPLER.bareColumn(x + k, z, true).land > 20;
            }
            if (!land) continue;
            for (int k = 1; k < 64; k++) {
                sum += Math.abs(top[k + 1] - 2 * top[k] + top[k - 1]);
                n++;
            }
        }
        assertTrue(n > 2000, "sampled " + n);
        return sum / n;
    }

    @Test
    void crestsAreSmooth() {
        // Four ridged octaves gave 0.075 here; three give 0.062.
        double r = roughness();
        assertTrue(r < 0.068, "roughness " + r);
    }
}

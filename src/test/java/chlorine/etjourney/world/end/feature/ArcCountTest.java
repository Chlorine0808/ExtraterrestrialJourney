package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** ARCS laid over another base draws a third as many paths as ARCS standing as the base. */
class ArcCountTest {

    private static int paths(long seed, ArcPaths.Probe probe) {
        int n = 0;
        for (int i = 0; i < 3000; i++) n += ArcPaths.pathsInCell(seed, 40 + i % 60, 40 + i / 60, probe)
            .size();
        return n;
    }

    @Test
    void anOverlayDrawsAThirdOfTheBasePaths() {
        ArcPaths.Probe base = new ArcPaths.Probe() {

            @Override
            public double weight(double x, double z) {
                return 1;
            }

            @Override
            public double baseWeight(double x, double z) {
                return 1;
            }
        };
        // An overlay at its full strength: the weight is all overlay, none of it base.
        ArcPaths.Probe overlay = new ArcPaths.Probe() {

            @Override
            public double weight(double x, double z) {
                return 0.8;
            }

            @Override
            public double baseWeight(double x, double z) {
                return 0;
            }
        };
        // Distinct seeds: the cell cache keys on the seed, not the probe.
        double ratio = paths(611L, overlay) / (double) paths(612L, base);
        assertTrue(ratio > 0.25 && ratio < 0.42, "overlay paths per base path " + ratio);
    }
}

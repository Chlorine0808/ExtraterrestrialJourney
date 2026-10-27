package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** A tube is made of the fill of the ground it touches most, and stays bare when it touches none. */
class ArcBiomeTest {

    /** A straight tube at Y 50, radius 6, from x = 0 to 100 along z = 0. */
    private static ArcPaths.Path straight(double z) {
        double[] points = new double[26 * 3];
        for (int i = 0; i <= 25; i++) {
            points[i * 3] = i * 4;
            points[i * 3 + 1] = 50;
            points[i * 3 + 2] = z;
        }
        return new ArcPaths.Path(points, 6);
    }

    /** Ground reaching the tube's underside below x < 30 (GROVE) and x >= 60 (AMBER), low ground between. */
    private static final ArcPaths.Ground GROUND = new ArcPaths.Ground() {

        @Override
        public double[] span(double x, double z) {
            return new double[] { 0, x >= 30 && x < 60 ? 40 : 47 };
        }

        @Override
        public EndBiome biome(double x, double z) {
            return x < 30 ? EndBiome.GROVE : EndBiome.AMBER;
        }
    };

    @Test
    void aTubeTakesTheGroundItTouchesMost() {
        assertEquals(EndBiome.AMBER, ArcPaths.groundBiome(straight(0), GROUND));
    }

    @Test
    void aTubeTouchingNoGroundStaysBare() {
        ArcPaths.Ground low = new ArcPaths.Ground() {

            @Override
            public double[] span(double x, double z) {
                return new double[] { 0, 40 };
            }

            @Override
            public EndBiome biome(double x, double z) {
                return EndBiome.GROVE;
            }
        };
        assertNull(ArcPaths.groundBiome(straight(0), low));
    }

    @Test
    void aTubeOverTheVoidStaysBare() {
        ArcPaths.Ground none = new ArcPaths.Ground() {

            @Override
            public double[] span(double x, double z) {
                return null;
            }

            @Override
            public EndBiome biome(double x, double z) {
                return EndBiome.GROVE;
            }
        };
        assertNull(ArcPaths.groundBiome(straight(0), none));
    }

    /** Each block inside a tube reads the biome of the tube it lies in. */
    @Test
    void segmentsCarryTheirTubesBiome() {
        ArcPaths.Ground byZ = new ArcPaths.Ground() {

            @Override
            public double[] span(double x, double z) {
                return new double[] { 0, 47 };
            }

            @Override
            public EndBiome biome(double x, double z) {
                return z < 50 ? EndBiome.GROVE : EndBiome.MYCELIA;
            }
        };
        ArcPaths.Segments segments = ArcPaths.segmentsNear(
            Arrays.asList(straight(0), straight(100)),
            -1e9,
            1e9,
            -1e9,
            1e9,
            null,
            byZ,
            (x, y, z) -> true);
        assertEquals(EndBiome.GROVE, ArcPaths.biomeAt(segments, 50, 50, 2));
        assertEquals(EndBiome.MYCELIA, ArcPaths.biomeAt(segments, 50, 52, 99));
        ArcPaths.Segments plain = ArcPaths
            .segmentsNear(Collections.singletonList(straight(0)), -1e9, 1e9, -1e9, 1e9, (x, y, z) -> true);
        assertNull(ArcPaths.biomeAt(plain, 50, 50, 0));
    }
}

package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The read-only query for where an arc tube first meets the ground, and not where ARCS has cut the tube off. */
class ArcTouchTest {

    private static ArcPaths.Ground ground(double bottom, double top) {
        return new ArcPaths.Ground() {

            @Override
            public double[] span(double x, double z) {
                return new double[] { bottom, top };
            }

            @Override
            public EndBiome biome(double x, double z) {
                return null;
            }
        };
    }

    @Test
    void touchesWhereTheTubeMeetsTheGround() {
        ArcPaths.Path p = new ArcPaths.Path(new double[] { 0, 100, 0, 4, 52, 0, 8, 52, 0 }, 7.5);
        assertArrayEquals(new double[] { 4, 52, 0 }, ArcPaths.touchPoint(p, ground(40, 48), (x, z) -> 1), 1e-9);
    }

    @Test
    void noTouchHighAboveTheGroundOrWhereTheTubeIsCutOff() {
        ArcPaths.Path high = new ArcPaths.Path(new double[] { 0, 100, 0, 4, 100, 0 }, 7.5);
        assertNull(ArcPaths.touchPoint(high, ground(40, 48), (x, z) -> 1));
        ArcPaths.Path low = new ArcPaths.Path(new double[] { 0, 50, 0, 4, 50, 0 }, 7.5, 0.2);
        assertNull(ArcPaths.touchPoint(low, ground(40, 48), (x, z) -> 0.1));
    }
}

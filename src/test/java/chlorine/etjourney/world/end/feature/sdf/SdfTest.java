package chlorine.etjourney.world.end.feature.sdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The signed distance shapes: negative inside, zero on the surface, positive outside. */
class SdfTest {

    private static final double EPS = 1e-9;

    @Test
    void sphereMeasuresFromItsSurface() {
        Sdf s = Sdf.sphere(3);
        assertEquals(-3, s.distance(0, 0, 0), EPS);
        assertEquals(0, s.distance(0, 3, 0), EPS);
        assertEquals(2, s.distance(5, 0, 0), EPS);
    }

    @Test
    void lineIsACapsule() {
        Sdf s = Sdf.line(0, 0, 0, 0, 10, 0, 1);
        assertEquals(-1, s.distance(0, 5, 0), EPS);
        assertEquals(1, s.distance(2, 5, 0), EPS);
        assertEquals(1, s.distance(0, 12, 0), EPS);
    }

    @Test
    void cappedConeNarrowsFromBottomToTop() {
        // Height is the half height, as in BCLib: the cone runs from y = -2 (radius 4) to y = 2 (radius 1).
        Sdf s = Sdf.cappedCone(2, 4, 1);
        assertTrue(s.distance(3.5, -1.9, 0) < 0);
        assertTrue(s.distance(3.5, 1.9, 0) > 0);
        assertTrue(s.distance(0.5, 1.9, 0) < 0);
        assertTrue(s.distance(0, 2.5, 0) > 0);
        assertTrue(s.distance(0, -2.5, 0) > 0);
    }

    @Test
    void translateAndScaleMoveTheShape() {
        Sdf s = Sdf.sphere(1)
            .translate(5, 0, 0);
        assertEquals(-1, s.distance(5, 0, 0), EPS);
        Sdf flat = Sdf.sphere(2)
            .scale(1, 0.5, 1);
        assertTrue(flat.distance(1.9, 0, 0) < 0);
        assertTrue(flat.distance(0, 1.1, 0) > 0);
    }

    @Test
    void booleansCombineShapes() {
        Sdf a = Sdf.sphere(2), b = Sdf.sphere(2)
            .translate(3, 0, 0);
        assertTrue(
            a.union(b)
                .distance(3, 0, 0) < 0);
        assertTrue(
            a.subtract(b)
                .distance(1.5, 0, 0) > 0);
        assertTrue(
            a.subtract(b)
                .distance(-1, 0, 0) < 0);
        // The smooth union fills the neck between the two balls that the plain union leaves open.
        assertTrue(
            a.union(b)
                .distance(1.5, 1.6, 0) > 0);
        assertTrue(
            a.smoothUnion(b, 2)
                .distance(1.5, 1.6, 0) < 0);
    }

    @Test
    void flatWaveRipplesAroundTheVerticalAxis() {
        Sdf s = Sdf.sphere(4)
            .flatWave(4, 1, 0);
        // cos(4 * atan2(x, z)) adds to the distance: the shape pulls in along the axes and bulges on the diagonals.
        assertTrue(s.distance(0, 0, 3.2) > 0);
        double d = 4.5 / Math.sqrt(2);
        assertTrue(s.distance(d, 0, d) < 0);
    }

    @Test
    void displaceAddsToTheDistance() {
        Sdf s = Sdf.sphere(2)
            .displace((x, y, z) -> -1);
        assertEquals(-3, s.distance(0, 0, 0), EPS);
        assertTrue(s.distance(2.5, 0, 0) < 0);
    }

    @Test
    void warpMovesThePointBeforeMeasuring() {
        Sdf s = Sdf.sphere(1)
            .warp(p -> p[1] -= 5);
        assertTrue(s.distance(0, 5, 0) < 0);
        assertTrue(s.distance(0, 0, 0) > 0);
    }

    @Test
    void uniformScaleKeepsDistance() {
        assertEquals(3, Sdf.sphere(2).scale(3).distance(9, 0, 0), EPS);
    }

    @Test
    void roundGrows() {
        assertEquals(0, Sdf.sphere(2).round(1.5).distance(3.5, 0, 0), EPS);
    }
}

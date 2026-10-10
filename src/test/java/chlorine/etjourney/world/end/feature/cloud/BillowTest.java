package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A billow is a curling sheet rooted in the ground, open at its eye and cut off at its length. */
class BillowTest {

    private static double densityAt(Billow b, double along, double p, double q) {
        double[] w = b.at(along, p, q);
        return b.density(w[0], w[1], w[2]);
    }

    @Test
    void aCurlRootedInTheGround() {
        Billow b = Probes.first(Billow.KIND, 571L, Probes.LAND);
        assertNotNull(b);
        assertTrue(b.footprint <= 90, "footprint " + b.footprint);
        assertTrue(densityAt(b, 0, 0, -b.radius) > 0, "root");
        assertTrue(densityAt(b, 0, 0, 0) < 0, "eye");
        assertTrue(densityAt(b, b.length / 2 + 2, 0, -b.radius) < 0, "past the end");
        assertTrue(b.eye - b.radius <= 70, "the root reaches the ground");
    }

    @Test
    void noBillowOverTheVoid() {
        assertEquals(0, Probes.count(Billow.KIND, 572L, Probes.VOID));
    }
}

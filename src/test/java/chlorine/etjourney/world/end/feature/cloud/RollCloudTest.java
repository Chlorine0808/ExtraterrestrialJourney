package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A roll cloud is a long lying tube, closed at both ends, and sometimes hollow enough to walk through. */
class RollCloudTest {

    @Test
    void aLongTubeClosedAtTheEnds() {
        RollCloud r = Probes.first(RollCloud.KIND, 531L, Probes.VOID);
        assertNotNull(r);
        assertTrue(r.footprint <= 220, "footprint " + r.footprint);
        assertTrue(r.length >= 200);
        double[] mid = r.axis(0.5), end = r.axis(1), before = r.axis(0.98);
        // The wall at mid length is solid: on the axis when filled, just inside the surface when hollow.
        double inset = r.hollow ? r.radius - 4 : 0;
        assertTrue(r.density(mid[0], mid[1] + inset, mid[2]) > 0, "wall");
        // Beyond the end, along the axis, there is air.
        double dx = end[0] - before[0], dz = end[2] - before[2], d = Math.hypot(dx, dz);
        assertTrue(r.density(end[0] + dx / d * r.radius, end[1], end[2] + dz / d * r.radius) < 0, "end");
    }

    @Test
    void someAreHollow() {
        int hollow = 0, all = 0;
        int c0 = Probes.cell0(RollCloud.KIND);
        for (int i = 0; i < 400; i++) {
            RollCloud r = RollCloud.KIND.inCell(532L, c0 + i % 20, c0 + i / 20, Probes.VOID);
            if (r == null) continue;
            all++;
            if (r.hollow) {
                hollow++;
                double[] mid = r.axis(0.5);
                assertTrue(r.density(mid[0], mid[1], mid[2]) < 0, "the tunnel is open");
            }
        }
        assertTrue(hollow > 0 && hollow < all, hollow + " of " + all);
    }

    @Test
    void theLongestWidestRollStaysWithinTheDeclaredFootprint() {
        RollCloud r = new RollCloud(0, 0, 100, new double[6], 380, 16, false, 0);
        assertTrue(r.footprint <= RollCloud.MAX_FOOTPRINT, "footprint " + r.footprint);
    }
}

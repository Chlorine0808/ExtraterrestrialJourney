package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An anvil is a stalk from the ground under a flat cap that drifts to one side. */
class AnvilTest {

    @Test
    void stalkAndDriftingCap() {
        Anvil a = Probes.first(Anvil.KIND, 521L, Probes.LAND);
        assertNotNull(a);
        assertTrue(a.footprint <= 145, "footprint " + a.footprint);
        assertTrue(a.density(a.centreX, a.base + 10, a.centreZ) > 0, "stalk");
        assertTrue(a.density(a.capX, a.capY + a.thick / 2, a.capZ) > 0, "cap");
        assertTrue(a.capY - a.base >= 60);
        // Under the far side of the cap there is only air.
        double dx = a.capX - a.centreX, dz = a.capZ - a.centreZ, d = Math.hypot(dx, dz);
        double fx = a.capX + dx / d * a.radius * 0.7, fz = a.capZ + dz / d * a.radius * 0.7;
        assertTrue(a.density(fx, a.capY - 5, fz) < 0, "under the cap");
    }

    /** Height of the highest solid point of the column, or NaN when it has none. */
    private static double top(Anvil a, double x, double z) {
        for (double y = a.maxY(); y >= a.capY - 5; y -= 0.25) if (a.density(x, y, z) >= 0) return y;
        return Double.NaN;
    }

    @Test
    void capIsFlatOnTopAndRoundAtTheRim() {
        for (long seed = 530L; seed < 536L; seed++) {
            Anvil a = Probes.first(Anvil.KIND, seed, Probes.LAND);
            assertNotNull(a);
            double middle = top(a, a.capX, a.capZ);
            // Away from the rim and the stalk the top stays level, so it shows no terraces.
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4;
                double x = a.capX + Math.cos(angle) * a.radius * 0.4, z = a.capZ + Math.sin(angle) * a.radius * 0.4;
                assertEquals(middle, top(a, x, z), 0.3, "level top at angle " + k);
            }
            // The last solid column along a ray sits clearly lower: the rim rounds off.
            for (int k = 0; k < 8; k++) {
                double angle = k * Math.PI / 4, edge = Double.NaN;
                for (double r = a.radius * 0.4; r < a.radius * 2; r += 0.25) {
                    double t = top(a, a.capX + Math.cos(angle) * r, a.capZ + Math.sin(angle) * r);
                    if (Double.isNaN(t)) break;
                    edge = t;
                }
                assertTrue(edge <= middle - 2, "rim at angle " + k + " is " + (middle - edge) + " below the top");
            }
        }
    }
}

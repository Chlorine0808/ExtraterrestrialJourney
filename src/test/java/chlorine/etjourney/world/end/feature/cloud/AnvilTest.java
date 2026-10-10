package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** An anvil is a stalk from the ground under a flat cap that drifts to one side. */
class AnvilTest {

    @Test
    void stalkAndDriftingCap() {
        Anvil a = Probes.first(Anvil.KIND, 521L, Probes.LAND);
        assertNotNull(a);
        assertTrue(a.footprint <= 101, "footprint " + a.footprint);
        assertTrue(a.density(a.centreX, a.base + 10, a.centreZ) > 0, "stalk");
        assertTrue(a.density(a.capX, a.capY + a.thick / 2, a.capZ) > 0, "cap");
        assertTrue(a.capY - a.base >= 60);
        // Under the far side of the cap there is only air.
        double dx = a.capX - a.centreX, dz = a.capZ - a.centreZ, d = Math.hypot(dx, dz);
        double fx = a.capX + dx / d * a.radius * 0.7, fz = a.capZ + dz / d * a.radius * 0.7;
        assertTrue(a.density(fx, a.capY - 5, fz) < 0, "under the cap");
    }
}

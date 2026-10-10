package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pouches hang from the underside of the land, sunk into it, and nowhere else. */
class MammatusTest {

    @Test
    void hangsFromTheUnderside() {
        Mammatus m = Probes.first(Mammatus.KIND, 511L, Probes.LAND);
        assertNotNull(m);
        assertTrue(m.density(m.centreX, m.centreY, m.centreZ) > 0);
        // No gap between the pouch and the land above it.
        assertTrue(m.density(m.centreX, 50, m.centreZ) > 0);
        assertTrue(m.density(m.centreX, m.centreY - m.radius - 1, m.centreZ) < 0);
    }

    @Test
    void noPouchWithoutAnUnderside() {
        assertEquals(0, Probes.count(Mammatus.KIND, 512L, Probes.VOID));
        assertEquals(0, Probes.count(Mammatus.KIND, 513L, Probes.of(1, 60, 70, 30)));
    }
}

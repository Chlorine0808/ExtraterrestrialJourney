package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A cumulus is a solid heap of puffs on a flat base, floating clear of the ground. */
class CumulusTest {

    @Test
    void solidAboveAFlatBase() {
        Cumulus c = Probes.first(Cumulus.KIND, 501L, Probes.LAND);
        assertNotNull(c);
        assertTrue(c.footprint <= 48, "footprint " + c.footprint);
        assertTrue(c.bottom >= 70 + 30, "base " + c.bottom);
        assertTrue(c.density(c.centreX, c.centreY, c.centreZ) > 0);
        // At least 8 blocks of body over the base on the axis, so the density grid keeps it.
        assertTrue(c.density(c.centreX, c.bottom + 8, c.centreZ) > 0);
        assertTrue(c.density(c.centreX, c.bottom - 1, c.centreZ) < 0);
    }

    @Test
    void floatsOverTheVoidToo() {
        Cumulus c = Probes.first(Cumulus.KIND, 502L, Probes.VOID);
        assertNotNull(c);
        assertTrue(c.bottom >= 40, "base " + c.bottom);
    }
}

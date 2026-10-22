package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Lens-shaped discs stack with gaps between them and thin out towards their rims. */
class LenticularTest {

    /** Solid blocks on the vertical line at (x, z) through disc i, as the voxel pass decides them. */
    private static int blocks(Lenticular l, double x, double z, int i) {
        int n = 0;
        double c = l.discCentre(i);
        for (int y = (int) Math.floor(c - 5); y <= c + 5; y++) if (l.density(x, y + 0.5, z) >= 0) n++;
        return n;
    }

    @Test
    void stackedLensesWithGaps() {
        Lenticular l = Probes.first(Lenticular.KIND, 541L, Probes.VOID);
        assertNotNull(l);
        assertTrue(l.discs >= 3 && l.discs <= 5);
        assertTrue(l.bottom >= 80 && l.maxY() <= 221, l.bottom + ".." + l.maxY());
        int centre = blocks(l, l.centreX, l.centreZ, 0);
        assertTrue(centre >= 3 && centre <= 7, "centre thickness " + centre);
        assertTrue(blocks(l, l.centreX + l.radius * 0.9, l.centreZ, 0) < centre, "rim");
        double gap = (l.discCentre(0) + l.discCentre(1)) / 2;
        assertTrue(l.density(l.centreX, gap, l.centreZ) < 0, "gap");
    }
}

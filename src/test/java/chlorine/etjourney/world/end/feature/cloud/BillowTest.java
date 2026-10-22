package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A billow is a curling sheet floating clear of the ground, open at its eye and cut off at its length. */
class BillowTest {

    private static double densityAt(Billow b, double along, double p, double q) {
        double[] w = b.at(along, p, q);
        return b.density(w[0], w[1], w[2]);
    }

    @Test
    void aCurlOpenAtItsEye() {
        Billow b = Probes.first(Billow.KIND, 571L, Probes.LAND);
        assertNotNull(b);
        assertTrue(b.footprint <= 103, "footprint " + b.footprint);
        assertTrue(densityAt(b, 0, 0, -b.radiusAt(0)) > 0, "bottom of the curl");
        assertTrue(densityAt(b, 0, 0, 0) < 0, "eye");
        assertTrue(densityAt(b, b.length / 2 + 2, 0, -b.radiusAt(0)) < 0, "past the end");
    }

    @Test
    void aSheetAtMostThreeBlocksThick() {
        int c0 = Probes.cell0(Billow.KIND);
        for (int i = 0; i < 400; i++) {
            Billow b = Billow.KIND.inCell(575L, c0 + i % 20, c0 + i / 20, Probes.LAND);
            if (b == null) continue;
            assertTrue(densityAt(b, 0, 0, -b.radiusAt(0) - 1.5) < 0, "below the bottom of the curl");
            assertTrue(densityAt(b, 0, 0, -b.radiusAt(0) + 1.5) < 0, "above the bottom of the curl");
        }
    }

    @Test
    void aboutHalfAreWorn() {
        int all = 0, worn = 0, c0 = Probes.cell0(Billow.KIND);
        for (int i = 0; i < 1600; i++) {
            Billow b = Billow.KIND.inCell(576L, c0 + i % 40, c0 + i / 40, Probes.LAND);
            if (b == null) continue;
            all++;
            if (b.wear() > 0) worn++;
        }
        assertTrue(all > 20, "formed " + all);
        double share = worn / (double) all;
        assertTrue(share > 0.35 && share < 0.65, "worn share " + share);
    }

    @Test
    void floatsClearOfTheGround() {
        int c0 = Probes.cell0(Billow.KIND);
        for (int i = 0; i < 400; i++) {
            Billow b = Billow.KIND.inCell(573L, c0 + i % 20, c0 + i / 20, Probes.LAND);
            if (b != null) assertTrue(b.minY() >= 70 + Clouds.OVER_GROUND - 1, "bottom " + b.minY());
        }
    }

    @Test
    void floatsOverTheVoidToo() {
        assertTrue(Probes.count(Billow.KIND, 572L, Probes.VOID) > 0);
    }

    @Test
    void windsPointEveryWay() {
        boolean[] quarters = new boolean[4];
        int c0 = Probes.cell0(Billow.KIND);
        for (int i = 0; i < 400; i++) {
            Billow b = Billow.KIND.inCell(574L, c0 + i % 20, c0 + i / 20, Probes.LAND);
            if (b == null) continue;
            double a = (b.wind % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
            quarters[(int) (a / (Math.PI / 2)) % 4] = true;
        }
        for (int q = 0; q < 4; q++) assertTrue(quarters[q], "no wind in quarter " + q);
    }
}

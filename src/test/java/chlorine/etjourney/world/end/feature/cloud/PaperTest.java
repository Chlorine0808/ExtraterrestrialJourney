package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** STRATUS papers are small thin sheets with sharp edges, floating clear of the ground, dense in some places. */
class PaperTest {

    /** Solid blocks on the vertical line through a point of the paper, as the voxel pass decides them. */
    private static int thickness(Paper p, double u, double w) {
        double[] s = p.at(u, w);
        int n = 0;
        for (int y = (int) Math.floor(s[1]) - 6; y <= s[1] + 6; y++) {
            if (p.density(s[0], y + 0.5, s[2]) >= 0) n++;
        }
        return n;
    }

    @Test
    void aThinSheetWithSharpEdges() {
        Paper p = Probes.first(Paper.KIND, 581L, Probes.LAND);
        assertNotNull(p);
        assertTrue(p.length >= 8 && p.length <= 24, "length " + p.length);
        assertTrue(p.footprint <= 18, "footprint " + p.footprint);
        int t = thickness(p, 0, 0);
        assertTrue(t >= 1 && t <= 3, "thickness " + t);
        // Solid right up to the corner, and nothing just past the edge.
        assertTrue(thickness(p, p.length / 2 - 0.6, p.width / 2 - 0.6) >= 1, "corner");
        assertTrue(thickness(p, p.length / 2 + 1, 0) == 0, "past the end");
        assertTrue(thickness(p, 0, p.width / 2 + 1) == 0, "past the side");
    }

    @Test
    void papersFloatClearOfTheGround() {
        for (int i = 0; i < 400; i++) {
            int c0 = Probes.cell0(Paper.KIND);
            Paper p = Paper.KIND.inCell(582L, c0 + i % 20, c0 + i / 20, Probes.LAND);
            if (p != null) assertTrue(p.minY() >= 70 + 3, "lowest " + p.minY());
        }
    }

    @Test
    void densityVariesFromPlaceToPlace() {
        // Papers per block of 10 x 10 cells over a wide area: some blocks crowded, some nearly empty.
        int c0 = Probes.cell0(Paper.KIND), most = 0, least = Integer.MAX_VALUE;
        for (int bx = 0; bx < 8; bx++) {
            for (int bz = 0; bz < 8; bz++) {
                int n = 0;
                for (int i = 0; i < 100; i++) {
                    if (Paper.KIND.inCell(583L, c0 + bx * 10 + i % 10, c0 + bz * 10 + i / 10, Probes.LAND) != null) n++;
                }
                most = Math.max(most, n);
                least = Math.min(least, n);
            }
        }
        assertTrue(most >= 3 * Math.max(1, least), most + " in the densest block, " + least + " in the sparsest");
    }
}

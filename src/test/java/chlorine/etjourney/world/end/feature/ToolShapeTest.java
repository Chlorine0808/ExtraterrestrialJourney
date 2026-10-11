package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Each tool shape is a solid in one piece, with a round handle and a head thicker than it. */
class ToolShapeTest {

    private static final Set<ToolShape> HAFTED = EnumSet
        .of(ToolShape.PICKAXE, ToolShape.AXE, ToolShape.HOE, ToolShape.SHOVEL, ToolShape.HAMMER, ToolShape.PAXEL);

    @Test
    void nineTools() {
        assertEquals(9, ToolShape.values().length);
    }

    @Test
    void boundsHoldTheShape() {
        for (ToolShape t : ToolShape.values()) {
            double[] b = t.bounds();
            int filled = 0;
            for (double x = -14; x <= 14; x += 0.5) {
                for (double y = -3; y <= 28; y += 0.5) {
                    for (double z = -8; z <= 8; z += 0.5) {
                        if (!t.inside(x, y, z)) continue;
                        filled++;
                        assertTrue(
                            x >= b[0] && x <= b[1] && y >= b[2] && y <= b[3] && z >= b[4] && z <= b[5],
                            t + " outside its bounds at " + x + "," + y + "," + z);
                    }
                }
            }
            assertTrue(filled > 500, t + " has " + filled + " cells");
        }
    }

    @Test
    void onePieceEach() {
        for (ToolShape t : ToolShape.values()) {
            double[] b = t.bounds();
            double step = 0.5;
            int nx = (int) ((b[1] - b[0]) / step) + 1, ny = (int) ((b[3] - b[2]) / step) + 1,
                nz = (int) ((b[5] - b[4]) / step) + 1;
            boolean[][][] solid = new boolean[nx][ny][nz];
            int filled = 0, sx = -1, sy = -1, sz = -1;
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    for (int k = 0; k < nz; k++) {
                        solid[i][j][k] = t.inside(b[0] + i * step, b[2] + j * step, b[4] + k * step);
                        if (!solid[i][j][k]) continue;
                        filled++;
                        sx = i;
                        sy = j;
                        sz = k;
                    }
                }
            }
            boolean[][][] seen = new boolean[nx][ny][nz];
            Deque<int[]> todo = new ArrayDeque<>();
            todo.push(new int[] { sx, sy, sz });
            seen[sx][sy][sz] = true;
            int reached = 0;
            while (!todo.isEmpty()) {
                int[] p = todo.pop();
                reached++;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        for (int dk = -1; dk <= 1; dk++) {
                            int i = p[0] + di, j = p[1] + dj, k = p[2] + dk;
                            if (i < 0 || i >= nx || j < 0 || j >= ny || k < 0 || k >= nz) continue;
                            if (seen[i][j][k] || !solid[i][j][k]) continue;
                            seen[i][j][k] = true;
                            todo.push(new int[] { i, j, k });
                        }
                    }
                }
            }
            assertEquals(filled, reached, t + " falls apart");
        }
    }

    @Test
    void roundHandles() {
        for (ToolShape t : HAFTED) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8, c = Math.cos(a), s = Math.sin(a);
                assertTrue(t.inside(0.8 * c, 8, 0.8 * s), t + " handle too thin");
                assertTrue(!t.inside(1.1 * c, 8, 1.1 * s), t + " handle not round");
            }
        }
    }

    @Test
    void headsThickerThanTheHandle() {
        for (ToolShape t : HAFTED) {
            double thickest = 0;
            for (double x = -12; x <= 12; x += 0.25) {
                for (double y = 14; y <= 25; y += 0.25) {
                    for (double z = 0; z <= 6; z += 0.25) {
                        if (t.inside(x, y, z)) thickest = Math.max(thickest, z);
                    }
                }
            }
            assertTrue(thickest >= 1.2, t + " head only " + thickest + " thick");
        }
    }

    @Test
    void theAxeBladeThinsToAnEdge() {
        assertTrue(ToolShape.AXE.inside(2, 17, 1.2));
        assertTrue(!ToolShape.AXE.inside(7, 17, 1.0));
        assertTrue(ToolShape.AXE.inside(7, 17, 0));
    }

    @Test
    void plantedEndFilled() {
        for (ToolShape t : ToolShape.values()) {
            double[] p = t.planted();
            assertTrue(t.inside(p[0], p[1], 0), t.name());
            double[] b = t.bounds();
            double end = t.tipFirst ? b[3] : b[2];
            assertTrue(Math.abs(p[1] - end) <= 1.5, t + " planted at " + p[1]);
        }
        assertTrue(ToolShape.SWORD.tipFirst);
        assertTrue(!ToolShape.PICKAXE.tipFirst);
    }
}

package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;

/**
 * Each tool sprite is a 16 x 16 silhouette in one piece, with its planted end filled, and is as thick as it is wide, as
 * a cross's beams are.
 */
class ToolSpriteTest {

    @Test
    void nineTools() {
        assertEquals(9, ToolSprite.values().length);
    }

    @Test
    void sixteenSquareAndFilledEnough() {
        for (ToolSprite t : ToolSprite.values()) {
            int filled = 0;
            for (int col = 0; col < 16; col++) for (int row = 0; row < 16; row++) if (t.filled(col, row)) filled++;
            assertTrue(filled >= 30 && filled <= 140, t + " has " + filled + " pixels");
            assertTrue(!t.filled(-1, 0) && !t.filled(16, 0) && !t.filled(0, -1) && !t.filled(0, 16), t.name());
        }
    }

    @Test
    void onePieceEach() {
        for (ToolSprite t : ToolSprite.values()) {
            int filled = 0, startCol = -1, startRow = -1;
            for (int col = 0; col < 16; col++) {
                for (int row = 0; row < 16; row++) {
                    if (!t.filled(col, row)) continue;
                    filled++;
                    startCol = col;
                    startRow = row;
                }
            }
            boolean[][] seen = new boolean[16][16];
            Deque<int[]> todo = new ArrayDeque<>();
            todo.push(new int[] { startCol, startRow });
            seen[startCol][startRow] = true;
            int reached = 0;
            while (!todo.isEmpty()) {
                int[] p = todo.pop();
                reached++;
                for (int dc = -1; dc <= 1; dc++) {
                    for (int dr = -1; dr <= 1; dr++) {
                        int c = p[0] + dc, r = p[1] + dr;
                        if (c < 0 || c > 15 || r < 0 || r > 15 || seen[c][r] || !t.filled(c, r)) continue;
                        seen[c][r] = true;
                        todo.push(new int[] { c, r });
                    }
                }
            }
            assertEquals(filled, reached, t + " falls apart");
        }
    }

    @Test
    void plantedNearItsCorner() {
        for (ToolSprite t : ToolSprite.values()) {
            int[] p = t.planted();
            assertTrue(t.filled(p[0], p[1]));
            double d = t.tipFirst ? Math.hypot(15 - p[0], p[1]) : Math.hypot(p[0], 15 - p[1]);
            assertTrue(d <= 2.5, t + " planted at " + p[0] + "," + p[1]);
        }
        assertTrue(ToolSprite.SWORD.tipFirst);
        assertTrue(!ToolSprite.PICKAXE.tipFirst);
    }

    @Test
    void asThickAsItIsWide() {
        for (ToolSprite t : ToolSprite.values()) {
            for (int col = 0; col < 16; col++) {
                for (int row = 0; row < 16; row++) {
                    double th = t.thickness(col, row);
                    if (!t.filled(col, row)) assertEquals(0, th, t + " " + col + "," + row);
                    else assertTrue(th >= 1 && th <= ToolSprite.MAX_THICKNESS, t + " " + col + "," + row + ": " + th);
                }
            }
        }
        // The pickaxe's handle is three pixels across a diagonal, about two pixels wide.
        for (int row = 8; row <= 12; row++) {
            for (int col = 0; col < 10; col++) {
                if (ToolSprite.PICKAXE.filled(col, row))
                    assertEquals(1.83, ToolSprite.PICKAXE.thickness(col, row), 0.01);
            }
        }
        // The broad hammer head is thicker than its handle, but no thicker than the cap.
        assertEquals(ToolSprite.MAX_THICKNESS, ToolSprite.HAMMER.thickness(10, 3), 1e-9);
        // A string one pixel wide is one pixel thick.
        assertEquals(1, ToolSprite.BOW.thickness(9, 6), 1e-9);
    }
}

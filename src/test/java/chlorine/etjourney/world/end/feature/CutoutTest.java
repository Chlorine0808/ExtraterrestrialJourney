package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A cut-out holds one block per position inside its ball and knows when every window has been copied. */
class CutoutTest {

    private static final Biosphere BALL = new Biosphere(1000.3, -500.7, 120.4, 33.5, 7);

    @Test
    void everyBlockInsideHasItsOwnSlot() {
        Cutout cut = new Cutout(BALL);
        int n = 0;
        for (int x = (int) Math.floor(BALL.minX()); x <= (int) Math.ceil(BALL.maxX()); x++) {
            for (int z = (int) Math.floor(BALL.minZ()); z <= (int) Math.ceil(BALL.maxZ()); z++) {
                for (int y = (int) Math.floor(BALL.minY()); y <= (int) Math.ceil(BALL.maxY()); y++) {
                    if (BALL.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                    assertTrue(cut.covers(x, y, z), "not covered: " + x + "," + y + "," + z);
                    assertEquals(0, cut.get(x, y, z), "slot shared at " + x + "," + y + "," + z);
                    cut.set(x, y, z, ++n);
                }
            }
        }
        assertTrue(n > 100000, "only " + n + " blocks inside");
    }

    @Test
    void doneAfterEveryWindow() {
        Cutout cut = new Cutout(BALL);
        for (int i = 1; i < BALL.windowCount(); i++) assertFalse(cut.windowDone());
        assertTrue(cut.windowDone());
    }
}

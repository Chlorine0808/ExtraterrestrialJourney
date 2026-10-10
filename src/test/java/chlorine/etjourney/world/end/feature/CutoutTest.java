package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void theCacheDropsTheOldestBeyondItsCapacity() {
        CutoutCache cache = new CutoutCache(2);
        Cutout a = new Cutout(BALL), b = new Cutout(BALL), c = new Cutout(BALL);
        cache.put(1, a);
        cache.put(2, b);
        assertSame(a, cache.get(1));
        cache.put(3, c);
        // 1 was used after 2, so 2 is the oldest.
        assertNull(cache.get(2));
        assertSame(a, cache.get(1));
        assertSame(c, cache.get(3));
        cache.remove(1);
        assertEquals(1, cache.size());
    }

    @Test
    void aBallThatFailedIsRememberedSoItIsNotRetried() {
        CutoutCache cache = new CutoutCache(2);
        assertFalse(cache.failed(7));
        cache.fail(7);
        assertTrue(cache.failed(7));
        assertNull(cache.get(7));
    }

    /** A cut-out or a failure of one world means nothing in the next, so a new seed empties the cache. */
    @Test
    void aNewSeedForgetsEverything() {
        CutoutCache cache = new CutoutCache(2);
        cache.forSeed(1L);
        cache.put(5, new Cutout(BALL));
        cache.fail(6);
        cache.forSeed(1L);
        assertEquals(1, cache.size());
        cache.forSeed(2L);
        assertEquals(0, cache.size());
        assertFalse(cache.failed(6));
    }
}

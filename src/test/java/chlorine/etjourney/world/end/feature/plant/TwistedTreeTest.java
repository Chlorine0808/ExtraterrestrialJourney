package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The HIGHLAND twisted tree: a trunk that wanders sideways at most one block per level, under a small crown. */
class TwistedTreeTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void staysWithinEight() {
        assertTrue(TwistedTree.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(TwistedTree.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v), "seed " + seed);
            assertTrue(v.maxDistance(X, Z) <= TwistedTree.INSTANCE.radius(), "seed " + seed);
            assertTrue(v.minY >= Y, "seed " + seed);
        }
    }

    @Test
    void staysBelowThirty() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            TwistedTree.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            assertTrue(v.height(Y) <= 30, "seed " + seed);
        }
    }

    @Test
    void trunkIsConnectedAndWanders() {
        boolean wandered = false;
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            TwistedTree.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            assertTrue(v.cells.get(Voxels.key(X, Y, Z)) == EndBlock.HIGHLAND_LOG, "seed " + seed);
            int levels = 0;
            for (int dy = 0; dy < 12; dy++) {
                boolean any = false, standing = dy == 0;
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        if (v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz)) != EndBlock.HIGHLAND_LOG) continue;
                        any = true;
                        if (v.cells.get(Voxels.key(X + dx, Y + dy - 1, Z + dz)) == EndBlock.HIGHLAND_LOG) {
                            standing = true;
                        }
                        if (dx != 0 || dz != 0) wandered = true;
                    }
                }
                if (any) {
                    levels++;
                    assertTrue(standing, "seed " + seed + " level " + dy);
                }
            }
            assertTrue(levels >= 5 && levels <= 9, "seed " + seed);
        }
        assertTrue(wandered);
    }

    @Test
    void hasLeavesAndLogs() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            TwistedTree.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            assertTrue(v.count(EndBlock.HIGHLAND_LEAVES) >= 8, "seed " + seed);
            assertTrue(v.count(EndBlock.HIGHLAND_LOG) >= 5, "seed " + seed);
        }
    }

    @Test
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(TwistedTree.INSTANCE.grow(new Random(seed), X, Y, Z, 7, v), "seed " + seed);
            assertTrue(v.height(Y) <= 6, "seed " + seed);
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(TwistedTree.INSTANCE.grow(new Random(1), X, Y, Z, 6, v));
        assertTrue(v.isEmpty());
    }
}

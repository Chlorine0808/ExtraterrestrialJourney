package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The AMBER lightroot: roots crawling over the ground around a glowing seed ball. */
class LightrootTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void staysWithinEight() {
        assertTrue(Lightroot.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 20, v), "seed " + seed);
            assertTrue(v.maxDistance(X, Z) <= Lightroot.INSTANCE.radius(), "seed " + seed);
            assertTrue(v.minY >= Y, "seed " + seed);
        }
    }

    @Test
    void staysBelowThirty() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            assertTrue(v.height(Y) <= 30, "seed " + seed);
        }
    }

    @Test
    void rootsHugTheGround() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 20, v);
            int roots = v.count(EndBlock.AMBER_ROOT);
            int low = 0;
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz)) == EndBlock.AMBER_ROOT) low++;
                    }
                }
            }
            assertTrue(roots >= 20, "seed " + seed);
            assertTrue(low * 10 >= roots * 8, "seed " + seed + ": " + low + " of " + roots);
        }
    }

    @Test
    void rootsReachOut() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 20, v);
            assertTrue(v.maxDistance(X, Z) >= 5, "seed " + seed);
        }
    }

    @Test
    void hasAGlowingSeed() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 20, v);
            assertTrue(v.count(EndBlock.AMBER_SEED) >= 4, "seed " + seed);
        }
    }

    @Test
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(Lightroot.INSTANCE.grow(new Random(seed), X, Y, Z, 4, v), "seed " + seed);
            assertTrue(v.height(Y) <= 3, "seed " + seed);
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(Lightroot.INSTANCE.grow(new Random(1), X, Y, Z, 3, v));
        assertTrue(v.isEmpty());
    }
}

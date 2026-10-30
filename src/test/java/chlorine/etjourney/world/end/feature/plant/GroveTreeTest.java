package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The GROVE tall tree and shrub: bounded by radius() per axis, by 30 blocks, and by the headroom they are given. */
class GroveTreeTest {

    private static final int X = 100, Y = 64, Z = -37;

    @Test
    void staysWithinItsRadius() {
        assertTrue(GroveTree.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(GroveTree.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v));
            assertTrue(v.maxDistance(X, Z) <= GroveTree.INSTANCE.radius(), "seed " + seed);
        }
    }

    @Test
    void staysBelowThirty() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            GroveTree.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            assertTrue(v.height(Y) <= 30, "seed " + seed);
        }
    }

    @Test
    void hasBlossomsInsideTheLeaves() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            GroveTree.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            int blossoms = v.count(EndBlock.GROVE_BLOSSOM);
            int leaves = blossoms + v.count(EndBlock.GROVE_LEAVES);
            assertTrue(blossoms >= 1, "seed " + seed);
            assertTrue(blossoms * 10 <= leaves, "seed " + seed);
            assertTrue(v.count(EndBlock.GROVE_LOG) > 0, "seed " + seed);
        }
    }

    @Test
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(GroveTree.INSTANCE.grow(new Random(seed), X, Y, Z, 18, v));
            assertTrue(v.height(Y) <= 16, "seed " + seed);
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(GroveTree.INSTANCE.grow(new Random(1), X, Y, Z, 10, v));
        assertTrue(v.isEmpty());
    }

    @Test
    void tallTreesHaveAThickTrunk() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            GroveTree.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            int base = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (v.cells.get(Voxels.key(X + dx, Y, Z + dz)) == EndBlock.GROVE_LOG) base++;
                }
            }
            assertEquals(v.height(Y) >= 20 ? 4 : 1, base, "seed " + seed);
        }
    }

    @Test
    void shrubFitsUnderFive() {
        assertTrue(GroveShrub.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(GroveShrub.INSTANCE.grow(new Random(seed), X, Y, Z, 5, v), "seed " + seed);
            assertTrue(v.height(Y) <= 4, "seed " + seed);
            assertTrue(v.maxDistance(X, Z) <= GroveShrub.INSTANCE.radius(), "seed " + seed);
            assertTrue(v.count(EndBlock.GROVE_LEAVES) > 0);
            assertEquals(0, v.count(EndBlock.GROVE_BLOSSOM));
        }
    }

    @Test
    void shrubRefusesWithoutRoom() {
        Voxels v = new Voxels();
        assertFalse(GroveShrub.INSTANCE.grow(new Random(1), X, Y, Z, 1, v));
        assertTrue(v.isEmpty());
        // Three blocks of air fit the lowest shrub, a stem and a bush top below the third.
        for (int seed = 0; seed < 100; seed++) {
            Voxels w = new Voxels();
            assertTrue(GroveShrub.INSTANCE.grow(new Random(seed), X, Y, Z, 3, w), "seed " + seed);
            assertTrue(w.height(Y) < 3, "seed " + seed);
        }
    }
}

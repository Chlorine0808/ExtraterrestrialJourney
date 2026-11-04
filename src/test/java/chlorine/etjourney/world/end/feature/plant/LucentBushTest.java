package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The Lucentwoods bush: a squashed ball of leaves on one log, furred on its top and sides. */
class LucentBushTest {

    private static final int X = 40, Y = 70, Z = -23;

    private static Voxels grow(int seed) {
        Voxels v = new Voxels();
        assertTrue(LucentBush.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v), "seed " + seed);
        return v;
    }

    @Test
    void writesStayWithinRadius() {
        assertTrue(LucentBush.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 200; seed++)
            assertTrue(grow(seed).maxDistance(X, Z) <= LucentBush.INSTANCE.radius(), "seed " + seed);
    }

    @Test
    void baseIsLogUnderLeaves() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = grow(seed);
            assertEquals(EndBlock.LUCENT_LOG, v.cells.get(Voxels.key(X, Y, Z)), "seed " + seed);
            assertEquals(EndBlock.LUCENT_LEAVES, v.cells.get(Voxels.key(X, Y + 1, Z)), "seed " + seed);
        }
    }

    @Test
    void furOnlyBesideLeavesAndNotBelow() {
        int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = grow(seed);
            int fur = 0;
            for (Map.Entry<Long, EndBlock> e : v.cells.entrySet()) {
                if (e.getValue() != EndBlock.LUCENT_FUR) continue;
                fur++;
                int x = Voxels.x(e.getKey()), y = Voxels.y(e.getKey()), z = Voxels.z(e.getKey());
                // Fur grows out of a leaf below it or beside it, never hangs under one.
                assertFalse(v.cells.get(Voxels.key(x, y + 1, z)) == EndBlock.LUCENT_LEAVES, "seed " + seed);
                boolean beside = false;
                for (int[] f : faces)
                    beside |= v.cells.get(Voxels.key(x + f[0], y + f[1], z + f[2])) == EndBlock.LUCENT_LEAVES;
                assertTrue(beside, "seed " + seed);
            }
            assertTrue(fur > 0, "seed " + seed);
        }
    }

    @Test
    void leavesReachWood() {
        for (int seed = 0; seed < 100; seed++)
            assertTrue(LeafReach.worst(grow(seed).cells, LeafReach::voxel) <= Grower.LEAF_REACH, "seed " + seed);
    }

    @Test
    void sameRandomSameShape() {
        assertEquals(grow(5).cells, grow(5).cells);
    }

    @Test
    void refusesWithoutHeadroom() {
        Voxels v = new Voxels();
        assertFalse(LucentBush.INSTANCE.grow(new Random(1), X, Y, Z, 2, v));
        assertTrue(v.isEmpty());
    }
}

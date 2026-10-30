package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Every leaf of the populate-window trees reaches its wood within Grower.LEAF_REACH face steps through leaves. */
class LeafReachTest {

    private static final int X = 100, Y = 64, Z = -37;

    private static void check(Grower grower, int headroom) {
        int worst = 0;
        for (int seed = 0; seed < 200; seed++) {
            Voxels v = new Voxels();
            if (!grower.grow(new Random(seed), X, Y, Z, headroom, v)) continue;
            int steps = LeafReach.worst(v.cells, LeafReach::voxel);
            worst = Math.max(worst, steps);
            assertTrue(
                steps <= Grower.LEAF_REACH,
                grower.getClass()
                    .getSimpleName() + " seed "
                    + seed
                    + ": "
                    + steps);
        }
        System.out.println(
            grower.getClass()
                .getSimpleName() + ": leaves at most "
                + worst
                + " steps from wood");
    }

    @Test
    void groveTreeLeavesReachWood() {
        check(GroveTree.INSTANCE, 64);
        check(GroveTree.INSTANCE, 18);
    }

    @Test
    void groveShrubLeavesReachWood() {
        check(GroveShrub.INSTANCE, 5);
    }

    @Test
    void twistedTreeLeavesReachWood() {
        check(TwistedTree.INSTANCE, 64);
    }
}

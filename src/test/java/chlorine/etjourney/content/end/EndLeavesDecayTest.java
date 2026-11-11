package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.plant.Grower;

class EndLeavesDecayTest {

    private static final int Y = 64;

    /** Leaves from x = 1 to x = run - 1 along the row, and wood at x = run. */
    private static FakeBlocks leafRowTo(int run) {
        FakeBlocks w = new FakeBlocks();
        for (int x = 1; x < run; x++) w.set(x, Y, 0, FakeBlocks.LEAVES);
        return w.set(run, Y, 0, FakeBlocks.WOOD);
    }

    @Test
    void woodAtTheFullReachThroughLeavesHolds() {
        assertTrue(EndLeavesBlock.isHeldByWood(leafRowTo(1), 0, Y, 0));
        assertTrue(EndLeavesBlock.isHeldByWood(leafRowTo(Grower.LEAF_REACH), 0, Y, 0));
    }

    @Test
    void woodOneStepBeyondTheReachDoesNot() {
        assertFalse(EndLeavesBlock.isHeldByWood(leafRowTo(Grower.LEAF_REACH + 1), 0, Y, 0));
    }

    @Test
    void airBreaksTheChain() {
        FakeBlocks w = leafRowTo(4).set(2, Y, 0, FakeBlocks.AIR);
        assertFalse(EndLeavesBlock.isHeldByWood(w, 0, Y, 0));
    }

    @Test
    void chainsTurnCorners() {
        FakeBlocks w = new FakeBlocks().set(0, Y + 1, 0, FakeBlocks.LEAVES)
            .set(0, Y + 1, 1, FakeBlocks.LEAVES)
            .set(-1, Y + 1, 1, FakeBlocks.WOOD);
        assertTrue(EndLeavesBlock.isHeldByWood(w, 0, Y, 0));
    }
}

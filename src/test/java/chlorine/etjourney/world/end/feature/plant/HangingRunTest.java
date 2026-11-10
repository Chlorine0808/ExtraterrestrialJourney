package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** A hanging run lands whole or not at all, and only in the half of its anchor. */
class HangingRunTest {

    private final Map<Long, EndBlock> cells = new HashMap<>();
    private final MapSink sink = new MapSink(cells, 0, 0, 16, 0, 256);
    private final HangingRun run = new HangingRun(sink, 8);

    private boolean has(int x, int y, int z) {
        return cells.containsKey(MapSink.key(x, y, z));
    }

    @Test
    void placedAnchorWritesEveryCellOverAir() {
        run.start(100, true);
        run.add(1, 99, 1, EndBlock.STONE);
        run.add(1, 98, 1, EndBlock.STONE);
        run.flush();
        assertTrue(has(1, 99, 1) && has(1, 98, 1));
    }

    @Test
    void unplacedAnchorWritesNothing() {
        run.start(100, false);
        run.add(1, 99, 1, EndBlock.STONE);
        run.flush();
        assertTrue(cells.isEmpty());
    }

    @Test
    void oneSolidCellInTheSinkCancelsTheWholeRun() {
        cells.put(MapSink.key(1, 97, 1), EndBlock.WATER);
        run.start(100, true);
        run.add(1, 99, 1, EndBlock.STONE);
        run.add(1, 98, 1, EndBlock.STONE);
        run.add(1, 97, 1, EndBlock.STONE);
        run.flush();
        assertFalse(has(1, 99, 1));
        assertFalse(has(1, 98, 1));
        assertEquals(EndBlock.WATER, cells.get(MapSink.key(1, 97, 1)));
    }

    @Test
    void cellsInTheOtherHalfAreCut() {
        // Anchored at the split, the run keeps its upper cells and drops the ones below it.
        run.start(HangingRun.SPLIT_Y, true);
        run.add(2, HangingRun.SPLIT_Y, 2, EndBlock.STONE);
        run.add(2, HangingRun.SPLIT_Y - 1, 2, EndBlock.STONE);
        run.flush();
        assertTrue(has(2, HangingRun.SPLIT_Y, 2));
        assertFalse(has(2, HangingRun.SPLIT_Y - 1, 2));
    }

    @Test
    void cellsOutsideTheSinkAreIgnoredNotBlocking() {
        run.start(100, true);
        run.add(1, 99, 1, EndBlock.STONE);
        run.add(40, 99, 1, EndBlock.STONE);
        run.flush();
        assertTrue(has(1, 99, 1));
        assertFalse(has(40, 99, 1));
    }

    @Test
    void neighbouringSinksEachWriteTheirOwnPartOfOneRun() {
        Map<Long, EndBlock> west = new HashMap<>(), east = new HashMap<>();
        for (int chunk = 0; chunk < 2; chunk++) {
            Map<Long, EndBlock> m = chunk == 0 ? west : east;
            HangingRun r = new HangingRun(new MapSink(m, chunk * 16, 0, 16, 0, 256), 4);
            r.start(100, true);
            r.add(15, 99, 3, EndBlock.STONE);
            r.add(16, 99, 3, EndBlock.STONE);
            r.flush();
        }
        assertTrue(west.containsKey(MapSink.key(15, 99, 3)) && !west.containsKey(MapSink.key(16, 99, 3)));
        assertTrue(east.containsKey(MapSink.key(16, 99, 3)) && !east.containsKey(MapSink.key(15, 99, 3)));
    }

    @Test
    void flushEmptiesTheRun() {
        run.start(100, true);
        run.add(1, 99, 1, EndBlock.STONE);
        run.flush();
        cells.clear();
        run.flush();
        assertTrue(cells.isEmpty());
    }
}

package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.plant.PlantCells.Bounds;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A shape touches a sink exactly when its box overlaps the sink volume; offsets pack to distinct keys. */
class PlantCellsTest {

    /** Sink x and z 0..15, y 10..19; box -2..3 on every axis. */
    private final MapSink sink = new MapSink(new HashMap<Long, EndBlock>(), 0, 0, 16, 10, 20);
    private final Bounds box = new Bounds() {

        public int minX() {
            return -2;
        }

        public int maxX() {
            return 3;
        }

        public int minY() {
            return -2;
        }

        public int maxY() {
            return 3;
        }

        public int minZ() {
            return -2;
        }

        public int maxZ() {
            return 3;
        }
    };

    @Test
    void xEdges() {
        assertTrue(PlantCells.touches(-3, 15, 8, box, sink));
        assertFalse(PlantCells.touches(-4, 15, 8, box, sink));
        assertTrue(PlantCells.touches(17, 15, 8, box, sink));
        assertFalse(PlantCells.touches(18, 15, 8, box, sink));
    }

    @Test
    void zEdges() {
        assertTrue(PlantCells.touches(8, 15, -3, box, sink));
        assertFalse(PlantCells.touches(8, 15, -4, box, sink));
        assertTrue(PlantCells.touches(8, 15, 17, box, sink));
        assertFalse(PlantCells.touches(8, 15, 18, box, sink));
    }

    @Test
    void yEdgesTreatMaxYAsExclusive() {
        assertTrue(PlantCells.touches(8, 7, 8, box, sink));
        assertFalse(PlantCells.touches(8, 6, 8, box, sink));
        assertTrue(PlantCells.touches(8, 21, 8, box, sink));
        assertFalse(PlantCells.touches(8, 22, 8, box, sink));
    }

    @Test
    void packGivesDistinctKeysAcrossTheRange() {
        Set<Long> keys = new HashSet<>();
        int count = 0;
        for (int dx = -256; dx < 256; dx += 51) {
            for (int dy = -512; dy < 512; dy += 103) {
                for (int dz = -256; dz < 256; dz += 51) {
                    keys.add(PlantCells.pack(dx, dy, dz));
                    count++;
                }
            }
        }
        // The extremes and their neighbours too.
        for (int d : new int[] { -255, 255 }) {
            keys.add(PlantCells.pack(d, -512, d));
            keys.add(PlantCells.pack(d, 511, d));
            count += 2;
        }
        assertEquals(count, keys.size());
    }
}

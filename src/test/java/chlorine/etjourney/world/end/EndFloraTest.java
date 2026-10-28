package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The populate window spans the populated chunk and its east, south and south-east neighbours, and nothing else. */
class EndFloraTest {

    @Test
    void theWindowFallsInTheFourChunksOnly() {
        for (int[] c : new int[][] { { 0, 0 }, { -1, -1 }, { 37, -52 } }) {
            int ox = c[0] * 16 + 8, oz = c[1] * 16 + 8;
            Set<Integer> used = new HashSet<>();
            for (int x = ox; x < ox + 16; x++) {
                for (int z = oz; z < oz + 16; z++) {
                    int i = EndFlora.chunkIndex(c[0], c[1], x, z);
                    assertTrue(i >= 0 && i < 4, x + "," + z);
                    assertEquals((x >> 4) - c[0], i >> 1);
                    assertEquals((z >> 4) - c[1], i & 1);
                    used.add(i);
                }
            }
            assertEquals(4, used.size());
        }
    }

    @Test
    void blocksOutsideTheFourChunksHaveNoIndex() {
        assertEquals(-1, EndFlora.chunkIndex(0, 0, -1, 4));
        assertEquals(-1, EndFlora.chunkIndex(0, 0, 4, -1));
        assertEquals(-1, EndFlora.chunkIndex(0, 0, 32, 4));
        assertEquals(-1, EndFlora.chunkIndex(0, 0, 4, 32));
        assertEquals(-1, EndFlora.chunkIndex(-1, -1, -17, -5));
    }
}

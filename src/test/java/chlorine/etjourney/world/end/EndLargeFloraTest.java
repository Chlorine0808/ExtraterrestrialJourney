package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The write check of EndLargeFlora: the populating chunk and its east, south and south-east neighbours only. */
class EndLargeFloraTest {

    @Test
    void readsAndWritesOnlyTheFourChunks() {
        // Chunk (2, 3) owns x 32..63 and z 48..79.
        assertTrue(EndLargeFlora.writable(2, 3, 32, 60, 48));
        assertTrue(EndLargeFlora.writable(2, 3, 63, 60, 79));
        assertFalse(EndLargeFlora.writable(2, 3, 31, 60, 48));
        assertFalse(EndLargeFlora.writable(2, 3, 64, 60, 48));
        assertFalse(EndLargeFlora.writable(2, 3, 32, 60, 47));
        assertFalse(EndLargeFlora.writable(2, 3, 32, 60, 80));
        assertTrue(EndLargeFlora.writable(2, 3, 40, 1, 50));
        assertTrue(EndLargeFlora.writable(2, 3, 40, 255, 50));
        assertFalse(EndLargeFlora.writable(2, 3, 40, 0, 50));
        assertFalse(EndLargeFlora.writable(2, 3, 40, 256, 50));
    }
}

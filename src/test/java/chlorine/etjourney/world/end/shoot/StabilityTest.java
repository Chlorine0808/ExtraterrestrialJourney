package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StabilityTest {

    @Test
    void settlesAfterTheNeededQuietFrames() {
        Stability s = new Stability(3, 2.0);
        int[] a = new int[16];
        assertFalse(s.offer(a));
        assertFalse(s.offer(a));
        assertFalse(s.offer(a));
        assertTrue(s.offer(a));
    }

    @Test
    void aChangeStartsTheCountAgain() {
        Stability s = new Stability(3, 2.0);
        int[] a = new int[16];
        int[] b = new int[16];
        java.util.Arrays.fill(b, 0xFF808080);
        s.offer(a);
        s.offer(a);
        s.offer(a);
        assertFalse(s.offer(b)); // big change resets the count
        assertFalse(s.offer(b));
        assertFalse(s.offer(b));
        assertTrue(s.offer(b));
    }

    @Test
    void smallDifferenceWithinToleranceCounts() {
        Stability s = new Stability(1, 2.0);
        int[] a = new int[4];
        int[] b = new int[4];
        java.util.Arrays.fill(b, 0x00020202); // mean abs diff 2.0
        s.offer(a);
        assertTrue(s.offer(b));
    }

    @Test
    void thumbnailAveragesBlocks() {
        int red = 0xFFFF0000, blue = 0xFF0000FF;
        int[] img = { red, red, blue, blue, red, red, blue, blue };
        assertArrayEquals(new int[] { red, blue }, Stability.thumbnail(img, 4, 2, 2, 1));
    }

    @Test
    void thumbnailMixesTwoColours() {
        int[] img = { 0xFF000000, 0xFF646464 };
        assertArrayEquals(new int[] { 0xFF323232 }, Stability.thumbnail(img, 2, 1, 1, 1));
    }
}

package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Cloud voxels are drawn, stay inside the sink, and come out the same in both halves of a column. */
class CloudVoxelsTest {

    static void assertHalvesAgree(String style) {
        int[] c = CloudSheetsTest.chunkOf(style);
        assertNotNull(c, "no " + style + " chunk found");
        MemorySink whole = CloudSheetsTest.draw(c, 0, 256, (a, b) -> Collections.emptyList());
        MemorySink low = CloudSheetsTest.draw(c, 0, 128, (a, b) -> Collections.emptyList());
        MemorySink high = CloudSheetsTest.draw(c, 128, 256, (a, b) -> Collections.emptyList());
        assertTrue(whole.placedOnlyWithinRange());
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    EndBlock half = y < 128 ? low.get(x, y, z) : high.get(x, y, z);
                    assertEquals(whole.get(x, y, z), half, "at " + x + "," + y + "," + z);
                }
            }
        }
    }

    @Test
    void bothHalvesDrawTheSameMackerel() {
        assertHalvesAgree("MACKEREL");
    }

    @Test
    void bothHalvesDrawTheSameVirga() {
        assertHalvesAgree("VIRGA");
    }

    @Test
    void bothHalvesDrawTheSameBillows() {
        assertHalvesAgree("BILLOWS");
    }
}

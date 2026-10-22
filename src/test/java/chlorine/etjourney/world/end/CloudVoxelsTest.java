package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.cloud.Anvil;
import chlorine.etjourney.world.end.feature.cloud.Cumulus;
import chlorine.etjourney.world.end.feature.cloud.Mammatus;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Cloud voxels are drawn, stay inside the sink, and come out the same in both halves of a column. */
class CloudVoxelsTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(91L, new RegionPicker(Styles.all()));

    /** A whole (unworn) structure of the kind near the first chunk of its style. */
    private static <T extends Structure> T unworn(Structure.Kind<T> kind) {
        int[] c = CloudSheetsTest.chunkOf(kind.style);
        assertNotNull(c, "no " + kind.style + " chunk found");
        StructureProbe probe = SAMPLER.structureProbe();
        for (T s : kind.near(91L, c[0] * 16 + 8, c[1] * 16 + 8, 600, probe)) if (s.wear() == 0) return s;
        return null;
    }

    /** The block pass itself draws the point, rather than leaving it to the 8-block density grid. */
    private static void assertDrawnAt(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        MemorySink sink = CloudSheetsTest
            .draw(new int[] { bx >> 4, bz >> 4 }, 0, 256, (a, b) -> Collections.emptyList());
        assertNotNull(sink.get(bx, by, bz), "nothing drawn at " + bx + "," + by + "," + bz);
    }

    @Test
    void anvilsAreDrawnBlockByBlock() {
        Anvil a = unworn(Anvil.KIND);
        assertNotNull(a);
        // Near the rim of the cap, where the grid would cut the round edge into polygons.
        assertDrawnAt(a.capX + a.radius - 2, a.capY + 1, a.capZ);
    }

    @Test
    void cumulusIsDrawnBlockByBlock() {
        Cumulus c = unworn(Cumulus.KIND);
        assertNotNull(c);
        assertDrawnAt(c.centreX, c.bottom + 1, c.centreZ);
    }

    @Test
    void mammatusIsDrawnBlockByBlock() {
        Mammatus m = unworn(Mammatus.KIND);
        assertNotNull(m);
        assertDrawnAt(m.centreX, m.centreY - m.radius + 1.5, m.centreZ);
    }

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
    void bothHalvesDrawTheSameLenticulars() {
        assertHalvesAgree("LENTICULARS");
    }

    @Test
    void bothHalvesDrawTheSameStratus() {
        assertHalvesAgree("STRATUS");
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

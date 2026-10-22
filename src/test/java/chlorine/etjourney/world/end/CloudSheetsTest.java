package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.cloud.Cirrus;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.builtin.CloudModifiers;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;

/** Sheet styles fill their sheets block by block, the same in both halves, and give way to reserved areas. */
class CloudSheetsTest {

    private static final long SEED = 91L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final ChunkPlan.ReservedLookup NONE = (a, b) -> Collections.emptyList();

    /** Chunks whose centre holds the style at 0.75 or more, up to limit of them; an overlay peaks at 0.8. */
    static List<int[]> chunksOf(String style, int limit) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < 300 && out.size() < limit; i++) {
            for (int j = 0; j < 300 && out.size() < limit; j++) {
                int cx = 100 + i * 3, cz = -450 + j * 3;
                if (SAMPLER.styleWeight(style, cx * 16 + 8, cz * 16 + 8) >= 0.75) out.add(new int[] { cx, cz });
            }
        }
        return out;
    }

    static int[] chunkOf(String style) {
        List<int[]> chunks = chunksOf(style, 1);
        return chunks.isEmpty() ? null : chunks.get(0);
    }

    static MemorySink draw(int[] c, int minY, int maxY, ChunkPlan.ReservedLookup reserved) {
        MemorySink sink = new MemorySink(c[0], c[1], minY, maxY);
        new ChunkPlan(SAMPLER, c[0], c[1], reserved).blocks(sink);
        return sink;
    }

    /** Blocks of the source's sheets that lie in the air above the ground, which the pass must have filled. */
    static int[] filledOf(String style, CloudModifiers.SheetSource source, MemorySink sink, int[] c) {
        ChunkPlan plan = new ChunkPlan(SAMPLER, c[0], c[1], NONE);
        int expected = 0, filled = 0;
        List<Layer> sheets = new ArrayList<>();
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                // Ground and weight as the pass sees them: from the built land and the chunk's grid columns.
                double[] in = CloudModifiers.sheetInputs(plan, c[0] * 16, c[1] * 16, style, x, z);
                double ground = in[0];
                sheets.clear();
                source.sheets(SEED, x, z, ground, in[1], sheets);
                for (Layer l : sheets) {
                    for (int y = Math.max((int) l.bottom, (int) Math.floor(ground) + 3); y <= l.top; y++) {
                        expected++;
                        if (sink.get(x, y, z) != null) filled++;
                    }
                }
            }
        }
        return new int[] { expected, filled };
    }

    /** The first chunks of the style that hold any of its sheets are filled wherever the sheets are. */
    static void assertDrawn(String style, CloudModifiers.SheetSource source) {
        // Streaky styles can miss a whole chunk, so look on until one holds a sheet.
        for (int[] c : chunksOf(style, 30)) {
            MemorySink sink = draw(c, 0, 256, NONE);
            assertTrue(sink.placedOnlyWithinRange());
            int[] n = filledOf(style, source, sink, c);
            if (n[0] == 0) continue;
            assertEquals(n[0], n[1], "holes in the " + style + " sheets");
            return;
        }
        fail("no " + style + " chunk holds a sheet");
    }

    static void assertHalvesAgree(String style) {
        int[] c = chunkOf(style);
        assertNotNull(c);
        MemorySink whole = draw(c, 0, 256, NONE), low = draw(c, 0, 128, NONE), high = draw(c, 128, 256, NONE);
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
    void sheetsGiveWayToReservedAreas() {
        // Streaks can miss a whole chunk, so look on until one holds them.
        for (int[] c : chunksOf("CIRRUS", 30)) {
            if (filledOf("CIRRUS", Cirrus::sheets, draw(c, 0, 256, NONE), c)[0] == 0) continue;
            Area island = new Area("hee", c[0] * 16 + 8, c[1] * 16 + 8, 400);
            MemorySink sink = draw(c, 0, 256, (a, b) -> Collections.singletonList(island));
            int[] n = filledOf("CIRRUS", Cirrus::sheets, sink, c);
            assertTrue(n[1] * 100 <= n[0], n[1] + " of " + n[0] + " sheet blocks inside a reserved area");
            return;
        }
        fail("no CIRRUS chunk holds a sheet");
    }

    @Test
    void cirrusSheetsAreDrawn() {
        assertDrawn("CIRRUS", Cirrus::sheets);
    }

    @Test
    void bothHalvesDrawTheSameCirrus() {
        assertHalvesAgree("CIRRUS");
    }

}

package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.cloud.Virga;

/**
 * Mammatus and virga hang from the rock above them without a gap, even where the underside slants or its lumps
 * leave it higher than the column's nominal underside.
 */
class HangingCloudsTest {

    /** Largest gap counted as floating; anything wider is open sky, not a gap under the slab. */
    private static final int GAP = 6;

    /**
     * {columns hanging from rock, columns floating just below it, streak roots with nothing above at all} over
     * chunks of the style. With streaks, a top that carries on up a column aside is part of a leaning streak, not
     * floating.
     */
    private static int[] hanging(String style, int chunks, boolean streaks) {
        int hanging = 0, floating = 0, dangling = 0;
        for (int[] c : CloudSheetsTest.chunksOf(style, chunks)) {
            MemorySink[] halves = ZoneIslandPaintTest.generate(c[0], c[1]);
            StructureProbe probe = ZoneIslandPaintTest.SAMPLER.structureProbe();
            ChunkPlan plan = new ChunkPlan(ZoneIslandPaintTest.SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList());
            for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
                for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                    for (int y = 1; y < 250; y++) {
                        // The top block of a cloud: drawn, not grid rock, with air above it.
                        if (get(halves, x, y, z) == null || plan.densityAt(x, y, z) > 0
                            || get(halves, x, y + 1, z) != null) continue;
                        if (streaks && leansOn(halves, x, y, z) || onRock(plan, x, y, z)) continue;
                        int gap = 1;
                        while (gap <= GAP * 2 && y + gap < 255 && get(halves, x, y + gap, z) == null) gap++;
                        if (gap > GAP * 2) {
                            // A streak's root, whole for its first blocks; frayed bits further down are shorter.
                            if (streaks && run(halves, x, y, z) >= 5
                                && !Virga.KIND.near(91L, x, z, 0, probe)
                                    .isEmpty())
                                dangling++;
                        } else if (gap <= GAP && plan.densityAt(x, y + gap, z) > 0) floating++;
                    }
                    for (int y = 1; y < 250; y++) {
                        if (get(halves, x, y, z) != null && plan.densityAt(x, y, z) <= 0
                            && plan.densityAt(x, y + 1, z) > 0) hanging++;
                    }
                }
            }
        }
        return new int[] { hanging, floating, dangling };
    }

    /** Whether the cloud carries on up within four blocks in a column aside, as a leaning or frayed streak does. */
    private static boolean leansOn(MemorySink[] halves, int x, int y, int z) {
        for (int dy = 1; dy <= 4 && y + dy < 256; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx != 0 || dz != 0) && inChunk(halves, x + dx, z + dz)
                        && get(halves, x + dx, y + dy, z + dz) != null) return true;
                }
            }
        }
        return false;
    }

    /** Whether the block lies on the land's rock, as the blocks a roughened face pushes out do. */
    private static boolean onRock(ChunkPlan plan, int x, int y, int z) {
        for (int dy = 1; dy <= 3 && y - dy >= 0; dy++) if (plan.densityAt(x, y - dy, z) > 0) return true;
        return false;
    }

    /** Blocks in the run going down from (x, y, z). */
    private static int run(MemorySink[] halves, int x, int y, int z) {
        int n = 0;
        while (y - n >= 0 && get(halves, x, y - n, z) != null) n++;
        return n;
    }

    private static boolean inChunk(MemorySink[] halves, int x, int z) {
        int ox = halves[0].originX(), oz = halves[0].originZ();
        return x >= ox && x < ox + 16 && z >= oz && z < oz + 16;
    }

    private static Object get(MemorySink[] halves, int x, int y, int z) {
        return y < 128 ? halves[0].get(x, y, z) : halves[1].get(x, y, z);
    }

    private static void assertHangs(String style, int chunks, int least, boolean streaks) {
        int[] h = hanging(style, chunks, streaks);
        assertTrue(h[0] > least, style + ": only " + h[0] + " columns hang from the rock");
        assertTrue(h[1] <= h[0] / 50, style + ": " + h[1] + " columns float under the rock, " + h[0] + " hang");
        assertEquals(0, h[2], style + ": streaks hang from nothing");
    }

    @Test
    void mammatusHangsFromTheRock() {
        assertHangs("MAMMATUS", 10, 100, false);
    }

    @Test
    void virgaHangsFromTheRock() {
        // Each streak is one column wide.
        assertHangs("VIRGA", 200, 30, true);
    }
}

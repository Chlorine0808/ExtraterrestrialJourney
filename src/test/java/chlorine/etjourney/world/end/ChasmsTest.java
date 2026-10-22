package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Canyons;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** CHASMS cut three times as deep as slot canyons and keep no floor, so they open onto the void. */
class ChasmsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static ChunkPlan plan(int cx, int cz) {
        return new ChunkPlan(SAMPLER, cx, cz, (x, z) -> Collections.emptyList());
    }

    /** A chunk deep inside a CHASMS region with a chasm crossing land. */
    private static int[] chasmChunk() {
        for (int n = 0; n < 200000; n++) {
            int cx = 80 + n % 400 * 3, cz = -900 + n / 400 * 3;
            double x = cx * 16 + 8, z = cz * 16 + 8;
            if (SAMPLER.styleWeight("CHASMS", x, z) < 0.75 || SAMPLER.land(x, z) <= 0) continue;
            for (int dx = 0; dx < 16; dx++) {
                if (Canyons.chasmDepth(SEED, cx * 16 + dx, cz * 16 + 8) > 0) return new int[] { cx, cz };
            }
        }
        throw new AssertionError("no CHASMS land found");
    }

    /** Stone from each column's bottom to its top, then the plan's block passes. */
    private static MemorySink cut(ChunkPlan plan, int cx, int cz, int minY, int maxY) {
        MemorySink sink = new MemorySink(cx, cz, minY, maxY);
        for (int x = cx * 16; x < cx * 16 + 16; x++) {
            for (int z = cz * 16; z < cz * 16 + 16; z++) {
                ColumnState c = plan.column(x, z);
                if (c.land <= 0) continue;
                for (int y = Math.max(minY, (int) c.bottom); y <= Math.min(maxY - 1, (int) c.top); y++) {
                    sink.set(x, y, z, EndBlock.STONE);
                }
            }
        }
        plan.blocks(sink);
        return sink;
    }

    @Test
    void chasmsAreThreeTimesAsDeepAsSlots() {
        double most = 0;
        for (int i = 0; i < 20000; i++) most = Math.max(most, Canyons.chasmDepth(SEED, 5000 + i, 3000 + i * 0.7));
        assertTrue(most > Canyons.MAX_DEPTH * 2 && most <= Canyons.MAX_DEPTH * 3, "deepest chasm " + most);
    }

    @Test
    void chasmsOpenOntoTheVoid() {
        int[] c = chasmChunk();
        ChunkPlan plan = plan(c[0], c[1]);
        MemorySink sink = cut(plan, c[0], c[1], 0, 256);
        int through = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                ColumnState col = plan.column(x, z);
                double depth = Canyons.chasmDepth(SEED, x, z);
                if (col.land <= 0 || depth < col.top - col.bottom + 2) continue;
                if (SAMPLER.styleWeight("CHASMS", x, z) < 0.7) continue;
                boolean open = true;
                for (int y = (int) col.bottom; y <= (int) col.top; y++) open &= sink.isAir(x, y, z);
                assertTrue(open, "rock left under a chasm at " + x + "," + z);
                through++;
            }
        }
        assertTrue(through > 0, "no chasm column deep enough to check");
    }

    @Test
    void bothHalvesCutTheSameBlocks() {
        int[] c = chasmChunk();
        ChunkPlan plan = plan(c[0], c[1]);
        MemorySink whole = cut(plan, c[0], c[1], 0, 256), low = cut(plan, c[0], c[1], 0, 128);
        MemorySink high = cut(plan, c[0], c[1], 128, 256);
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    EndBlock half = y < 128 ? low.get(x, y, z) : high.get(x, y, z);
                    assertEquals(whole.get(x, y, z), half, "at " + x + "," + y + "," + z);
                }
            }
        }
    }
}

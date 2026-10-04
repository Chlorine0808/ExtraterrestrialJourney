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

/** SLOT_CANYONS cuts its slots into real land, and both halves of a column cut the same blocks. */
class SlotCanyonsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static ChunkPlan plan(int cx, int cz) {
        return new ChunkPlan(SAMPLER, cx, cz, (x, z) -> Collections.emptyList());
    }

    /** A chunk deep inside a SLOT_CANYONS region with a slot crossing land. */
    private static int[] canyonChunk() {
        for (int r = 0; r < 200; r++) {
            for (int i = -r; i <= r; i++) {
                for (int j = -r; j <= r; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != r) continue;
                    int cx = 80 + i * 8, cz = j * 8;
                    double x = cx * 16 + 8, z = cz * 16 + 8;
                    if (SAMPLER.styleWeight("SLOT_CANYONS", x, z) < 0.75 || SAMPLER.land(x, z) <= 0) continue;
                    for (int dx = 0; dx < 16; dx++) {
                        if (Canyons.depth(SEED, cx * 16 + dx, cz * 16 + 8) > 0) return new int[] { cx, cz };
                    }
                }
            }
        }
        throw new AssertionError("no SLOT_CANYONS land found");
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
    void slotsAreCutIntoTheLand() {
        int[] c = canyonChunk();
        ChunkPlan plan = plan(c[0], c[1]);
        MemorySink sink = cut(plan, c[0], c[1], 0, 256);
        int cut = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                ColumnState col = plan.column(x, z);
                if (col.land <= 0 || Canyons.depth(SEED, x, z) <= 0) continue;
                if (sink.isAir(x, (int) col.top - 20, z)) cut++;
            }
        }
        assertTrue(cut > 0, "no column was cut 20 deep");
    }

    @Test
    void bothHalvesCutTheSameBlocks() {
        int[] c = canyonChunk();
        ChunkPlan plan = plan(c[0], c[1]);
        MemorySink whole = cut(plan, c[0], c[1], 0, 256);
        int split = (int) plan.column(c[0] * 16 + 8, c[1] * 16 + 8).top - 10;
        MemorySink low = cut(plan, c[0], c[1], 0, split), high = cut(plan, c[0], c[1], split, 256);
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    EndBlock half = y < split ? low.get(x, y, z) : high.get(x, y, z);
                    assertEquals(whole.get(x, y, z), half, "at " + x + "," + y + "," + z);
                }
            }
        }
    }
}

package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** A hole whose funnel climbs above Y 128 is carved the same whether a column is drawn whole or in two halves. */
class HoleHalvesTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final ChunkPlan.ReservedLookup NONE = (a, b) -> Collections.emptyList();

    /** A chunk with a funnel column (part cut) whose rock runs from below to above Y 128. */
    private static int[] funnelAcrossTheSplit() {
        for (int cx = 10; cx < 120; cx++) {
            for (int cz = -60; cz < 60; cz++) {
                Holes.Hole hole = Holes.inCell(SEED, cx, cz, SAMPLER.holeProbe());
                if (hole == null) continue;
                int r = (int) Math.ceil(hole.radius * 4) >> 4;
                int hx = (int) Math.floor(hole.x) >> 4, hz = (int) Math.floor(hole.z) >> 4;
                for (int i = -r; i <= r; i++) {
                    for (int j = -r; j <= r; j++) {
                        ChunkPlan plan = new ChunkPlan(SAMPLER, hx + i, hz + j, NONE);
                        for (int x = (hx + i) * 16; x < (hx + i) * 16 + 16; x += 3) {
                            for (int z = (hz + j) * 16; z < (hz + j) * 16 + 16; z += 3) {
                                double cut = Holes.cutFraction(SEED, plan.holes(), x, z);
                                if (cut <= 0.2 || cut >= 0.9) continue;
                                if (plan.densityAt(x, 140, z) > 0 && plan.densityAt(x, 110, z) > 0) {
                                    return new int[] { hx + i, hz + j };
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static MemorySink draw(int[] c, int minY, int maxY) {
        MemorySink sink = new MemorySink(c[0], c[1], minY, maxY);
        ChunkPlan plan = new ChunkPlan(SAMPLER, c[0], c[1], NONE);
        // The terrain as the generator and the tall pass lay it down, then the block passes.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (plan.densityAt(c[0] * 16 + x, y, c[1] * 16 + z) > 0) {
                        sink.set(c[0] * 16 + x, y, c[1] * 16 + z, EndBlock.STONE);
                    }
                }
            }
        }
        plan.blocks(sink);
        return sink;
    }

    @Test
    void bothHalvesCarveTheSameFunnel() {
        int[] c = funnelAcrossTheSplit();
        assertNotNull(c, "no funnel across Y 128 found");
        MemorySink whole = draw(c, 0, 256), low = draw(c, 0, 128), high = draw(c, 128, 256);
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

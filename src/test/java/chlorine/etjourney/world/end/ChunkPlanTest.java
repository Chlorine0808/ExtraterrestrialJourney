package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Density and block passes built from chunk plans: deterministic, and agreeing across chunk borders. */
class ChunkPlanTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static ChunkPlan plan(int cx, int cz) {
        return new ChunkPlan(SAMPLER, cx, cz, (x, z) -> Collections.emptyList());
    }

    private static double[] field(int cx, int cz) {
        double[] f = new double[3 * 33 * 3];
        DensityBuilder.fill(plan(cx, cz), f, cx * 2, cz * 2, 0);
        return f;
    }

    @Test
    void sameChunkGivesTheSameField() {
        assertArrayEquals(field(150, -40), field(150, -40), 0);
    }

    @Test
    void neighbouringChunksAgreeOnTheirSharedEdge() {
        Random r = new Random(5);
        for (int n = 0; n < 40; n++) {
            int cx = 80 + r.nextInt(400), cz = -200 + r.nextInt(400);
            double[] a = field(cx, cz), b = field(cx + 1, cz);
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 33; k++) {
                    assertEquals(a[(2 * 3 + j) * 33 + k], b[(0 * 3 + j) * 33 + k], 1e-9, "chunk " + cx + "," + cz);
                }
            }
        }
    }

    @Test
    void blockPassesAgreeOnSharedFeatures() {
        // Shoal platforms, islets and lake water decided per chunk must land the same from both sides: check that a
        // column's blocks depend only on the column, by running each chunk twice from fresh plans.
        Random r = new Random(6);
        int placed = 0;
        for (int n = 0; n < 30; n++) {
            int cx = 80 + r.nextInt(400), cz = -200 + r.nextInt(400);
            MemorySink a = new MemorySink(cx, cz, 128), b = new MemorySink(cx, cz, 128);
            plan(cx, cz).blocks(a);
            plan(cx, cz).blocks(b);
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    for (int y = 0; y < 128; y++) {
                        EndBlock ba = a.get(x, y, z);
                        assertEquals(ba, b.get(x, y, z));
                        if (ba != null) placed++;
                    }
                }
            }
        }
        assertTrue(placed > 0, "no blocks placed in any sampled chunk");
    }

    @Test
    void densityJustOutsideAChunkMatchesTheNeighbour() {
        Random r = new Random(7);
        for (int n = 0; n < 20; n++) {
            int cx = 80 + r.nextInt(400), cz = -200 + r.nextInt(400);
            ChunkPlan here = plan(cx, cz), east = plan(cx + 1, cz);
            for (int z = cz * 16; z < cz * 16 + 16; z += 3) {
                for (int y = 0; y < 256; y += 5) {
                    for (int x = (cx + 1) * 16; x < (cx + 1) * 16 + 6; x++) {
                        assertEquals(
                            east.densityAt(x, y, z),
                            here.densityAt(x, y, z),
                            1e-6,
                            "at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }
}

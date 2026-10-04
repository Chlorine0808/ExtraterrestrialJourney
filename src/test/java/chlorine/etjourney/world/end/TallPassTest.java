package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

class TallPassTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(31L, new RegionPicker(Styles.all()));

    /** First chunk along a row whose ground reaches well above the generator's ceiling. */
    private static ChunkPlan tallChunk() {
        for (int cx = 70; cx < 1500; cx++) {
            for (int cz = -40; cz <= 40; cz += 20) {
                ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (x, z) -> Collections.emptyList());
                // Ground above the old ceiling, not just a shape whose range reaches it.
                if (TallPass.needed(plan) && plan.column(cx * 16 + 8, cz * 16 + 8).top > 140) return plan;
            }
        }
        throw new AssertionError("no tall chunk found");
    }

    @Test
    void upperAndLowerGridsShareTheirY128Nodes() {
        ChunkPlan plan = tallChunk();
        double[] lower = new double[3 * 33 * 3];
        DensityBuilder.fill(plan, lower, plan.area().chunkX * 2, plan.area().chunkZ * 2, 0);
        double[] upper = TallPass.upperField(plan);
        for (int c = 0; c < 9; c++) assertEquals(lower[c * 33 + 32], upper[c * 33], 1e-9);
        AtomicInteger solid = new AtomicInteger();
        TallPass.forEachSolid(upper, (x, y, z) -> {
            assertTrue(y >= 128 && y < 256 && x >= 0 && x < 16 && z >= 0 && z < 16);
            solid.incrementAndGet();
        });
        assertTrue(solid.get() > 0);
    }
}

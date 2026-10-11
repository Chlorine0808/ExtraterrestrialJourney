package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Zone island tops never come out as bare end stone. The rock is laid from the 8 x 4 x 8 grid, so the paint
 * must follow the grid rather than the exact island density, which strays from it by a few blocks at the rim noise.
 */
class ZoneIslandPaintTest {

    private static final long SEED = 91L;
    static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    /** Both halves of a chunk as the game builds them: grid rock first, then the block pass. */
    static MemorySink[] generate(int cx, int cz) {
        ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
        MemorySink low = new MemorySink(cx, cz, 0, 128), high = new MemorySink(cx, cz, 128, 256);
        TallPass.forEachSolid(
            plan.densityField(false, true),
            (x, y, z) -> low.set(cx * 16 + x, y - TallPass.BASE_Y, cz * 16 + z, EndBlock.STONE));
        if (TallPass.needed(plan)) TallPass.forEachSolid(
            TallPass.upperField(plan),
            (x, y, z) -> { if (y <= 255) high.set(cx * 16 + x, y, cz * 16 + z, EndBlock.STONE); });
        plan.blocks(low);
        plan.blocks(high);
        return new MemorySink[] { low, high };
    }

    private static EndBlock get(MemorySink[] halves, int x, int y, int z) {
        return y < 128 ? halves[0].get(x, y, z) : halves[1].get(x, y, z);
    }

    @Test
    void islandTopsAreNeverBald() {
        int islands = 0, tops = 0;
        for (int cx = -40; cx < 40 && islands < 5; cx++) {
            for (int cz = -40; cz < 40 && islands < 5; cz++) {
                ZoneIslands.Island island = ZoneIslands.inCell(SEED, cx, cz, SAMPLER.landProbe());
                if (island == null) continue;
                islands++;
                List<ZoneIslands.Island> own = Collections.singletonList(island);
                int x0 = (int) (island.x - island.radius * 1.25), x1 = (int) (island.x + island.radius * 1.25);
                int z0 = (int) (island.z - island.radius * 1.25), z1 = (int) (island.z + island.radius * 1.25);
                for (int ccx = x0 >> 4; ccx <= x1 >> 4; ccx++) {
                    for (int ccz = z0 >> 4; ccz <= z1 >> 4; ccz++) {
                        MemorySink[] halves = generate(ccx, ccz);
                        ChunkPlan plan = new ChunkPlan(SAMPLER, ccx, ccz, (a, b) -> Collections.emptyList());
                        for (int x = ccx * 16; x < ccx * 16 + 16; x++) {
                            for (int z = ccz * 16; z < ccz * 16 + 16; z++) {
                                // The dome above the island's centre height, where only its own rock lies.
                                for (int y = (int) Math.ceil(island.y); y
                                    <= Math.min(254, island.y + island.up + 1); y++) {
                                    EndBlock b = get(halves, x, y, z);
                                    if (b == null || get(halves, x, y + 1, z) != null) continue;
                                    // Clouds and other structures may rest on the island; only the grid's rock counts.
                                    if (plan.densityAt(x, y, z) <= 0) continue;
                                    double d = ZoneIslands
                                        .density(own, x + 0.5, y + 0.5, z + 0.5, ZoneIslands.edge(SEED, x, z));
                                    if (d < -8) continue;
                                    tops++;
                                    assertNotEquals(
                                        EndBlock.STONE,
                                        b,
                                        "bald top at " + x + "," + y + "," + z + " (density " + d + ")");
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(tops > 1000, "only " + tops + " top blocks checked over " + islands + " islands");
    }
}

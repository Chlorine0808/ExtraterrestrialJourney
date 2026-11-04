package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Plants grown in the chunk pass root on the lake-shaped ground, never on the water. Seed 7037748451318983361 is the
 * 2026-11-04 shot whose MYCELIA lakes were roofed by glowshrooms rooted on the water.
 */
class LakePlantsTest {

    private static final long SEED = 7037748451318983361L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final int CELLS = 30;

    private static boolean trunk(EndBlock b) {
        return b == EndBlock.MYCELIA_STEM || b == EndBlock.GROVE_LOG || b == EndBlock.LUCENT_LOG;
    }

    private static List<Lakes.Lake> lakesIn(EndBiome biome, int max) {
        List<Lakes.Lake> out = new ArrayList<>();
        for (int i = -CELLS; i <= CELLS && out.size() < max; i++) {
            for (int j = -CELLS; j <= CELLS && out.size() < max; j++) {
                Lakes.Lake l = Lakes.inCell(SEED, i, j, SAMPLER.lakeProbe());
                if (l != null && SAMPLER.biome((int) Math.floor(l.x), (int) Math.floor(l.z)) == biome) out.add(l);
            }
        }
        return out;
    }

    /** Trunk blocks standing right on the lake's water, over every chunk the lake's flat ring reaches. */
    private static int trunksOnWater(Lakes.Lake lake) {
        double r = Lakes.floodRadius(lake);
        int c0x = (int) Math.floor((lake.x - r) / 16), c1x = (int) Math.floor((lake.x + r) / 16);
        int c0z = (int) Math.floor((lake.z - r) / 16), c1z = (int) Math.floor((lake.z + r) / 16);
        int found = 0;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                MemorySink sink = new MemorySink(cx, cz, 0, 128);
                ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
                double[] field = new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
                DensityBuilder.fill(plan, field, cx * 2, cz * 2, 0);
                final int ox = cx * 16, oz = cz * 16;
                TallPass
                    .forEachSolid(field, (x, y, z) -> sink.set(ox + x, y - TallPass.BASE_Y, oz + z, EndBlock.STONE));
                plan.blocks(sink);
                for (int x = ox; x < ox + 16; x++) {
                    for (int z = oz; z < oz + 16; z++) {
                        if (sink.get(x, lake.waterLevel, z) == EndBlock.WATER
                            && trunk(sink.get(x, lake.waterLevel + 1, z))) found++;
                    }
                }
            }
        }
        return found;
    }

    private static void check(EndBiome biome) {
        List<Lakes.Lake> lakes = lakesIn(biome, 2);
        assertFalse(lakes.isEmpty(), "no " + biome + " lake found");
        for (Lakes.Lake lake : lakes)
            assertEquals(0, trunksOnWater(lake), "trunks on the water of the lake at " + lake.x + "," + lake.z);
    }

    @Test
    void glowshroomsStayOffMyceliaLakes() {
        check(EndBiome.MYCELIA);
    }

    @Test
    void lucentTreesStayOffLucentwoodsLakes() {
        check(EndBiome.LUCENTWOODS);
    }
}

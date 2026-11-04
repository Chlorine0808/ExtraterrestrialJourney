package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Lakes;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Nothing that stands on the ground stands on a lake's water. Seed 7037748451318983361 is the 2026-11-04 shot whose
 * MYCELIA lakes were roofed by glowshrooms rooted on the water, with spires standing in them.
 */
class LakeWaterTest {

    private static final long SEED = 7037748451318983361L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final int CELLS = 30;
    /** Styles whose structures stand on the ground. */
    private static final String[] STANDING = { "SPIRES", "MUSHROOMS", "CROSSES", "HOLLOW_PILLARS", "SPIRAL_TOWERS",
        "ARCHES" };
    /** Kept off the shore, where a rock overhang may reach over the water. */
    private static final double SHORE = 3;

    private static boolean trunk(EndBlock b) {
        return b == EndBlock.MYCELIA_STEM || b == EndBlock.GROVE_LOG || b == EndBlock.LUCENT_LOG;
    }

    private static List<Lakes.Lake> lakes(Predicate<Lakes.Lake> wanted, int max) {
        List<Lakes.Lake> out = new ArrayList<>();
        for (int i = -CELLS; i <= CELLS && out.size() < max; i++) {
            for (int j = -CELLS; j <= CELLS && out.size() < max; j++) {
                Lakes.Lake l = Lakes.inCell(SEED, i, j, SAMPLER.lakeProbe());
                if (l != null && wanted.test(l)) out.add(l);
            }
        }
        return out;
    }

    private static boolean inBiome(Lakes.Lake l, EndBiome biome) {
        return SAMPLER.biome((int) Math.floor(l.x), (int) Math.floor(l.z)) == biome;
    }

    private static boolean underStructures(Lakes.Lake l) {
        for (String style : STANDING) if (SAMPLER.styleWeight(style, l.x, l.z) > 0.3) return true;
        return false;
    }

    /** Blocks of the given kind right on the lake's water, inside its shore, over every chunk the lake reaches. */
    private static int onWater(Lakes.Lake lake, Predicate<EndBlock> kind) {
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
                        if (Distance.planar(x + 0.5 - lake.x, z + 0.5 - lake.z) > lake.radius - SHORE) continue;
                        if (sink.get(x, lake.waterLevel, z) == EndBlock.WATER
                            && kind.test(sink.get(x, lake.waterLevel + 1, z))) found++;
                    }
                }
            }
        }
        return found;
    }

    private static void check(List<Lakes.Lake> lakes, Predicate<EndBlock> kind, String what) {
        assertFalse(lakes.isEmpty(), "no lake found for " + what);
        for (Lakes.Lake lake : lakes)
            assertEquals(0, onWater(lake, kind), what + " on the water of the lake at " + lake.x + "," + lake.z);
    }

    @Test
    void glowshroomsStayOffMyceliaLakes() {
        check(lakes(l -> inBiome(l, EndBiome.MYCELIA), 2), LakeWaterTest::trunk, "trunks");
    }

    @Test
    void lucentTreesStayOffLucentwoodsLakes() {
        check(lakes(l -> inBiome(l, EndBiome.LUCENTWOODS), 2), LakeWaterTest::trunk, "trunks");
    }

    @Test
    void standingStructuresStayOffLakes() {
        check(lakes(LakeWaterTest::underStructures, 6), b -> b == EndBlock.STONE, "structure stone");
    }
}

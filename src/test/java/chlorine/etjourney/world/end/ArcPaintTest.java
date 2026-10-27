package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Ore;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A tube that touches painted ground is made of that biome's fill all through; one that touches none stays stone. */
class ArcPaintTest {

    /** Rock that stands only through an arc tube, as the paint tells it apart from ground. */
    static boolean isTube(ChunkPlan plan, int x, int y, int z) {
        boolean upper = y >= 128;
        double terrain = DensityField.at(
            plan.densityField(upper, false),
            x - plan.area()
                .originX(),
            y - (upper ? 128 : 0),
            z - plan.area()
                .originZ());
        return terrain <= 0 && ArcPaths.density(plan.arcs(), x, y, z) > -3;
    }

    private static boolean isOre(EndBlock b) {
        for (Ore o : Ore.values()) if (o.block == b) return true;
        return false;
    }

    @Test
    void tubesAreMadeOfTheirGroundsFill() {
        TerrainSampler sampler = ZoneIslandPaintTest.SAMPLER;
        int painted = 0, bare = 0, wrong = 0, chunks = 0;
        String first = "";
        for (int step = 0; step < 40000 && (painted < 2000 || bare < 2000) && chunks < 300; step++) {
            int cx = (step % 200 - 100) * 5, cz = (step / 200 - 100) * 5;
            double mx = cx * 16 + 8, mz = cz * 16 + 8;
            if (mx * mx + mz * mz < 1200 * 1200 || sampler.styleWeight("ARCS", mx, mz) < 0.3) continue;
            ChunkPlan plan = new ChunkPlan(sampler, cx, cz, (a, b) -> Collections.emptyList());
            if (plan.arcs()
                .isEmpty()) continue;
            chunks++;
            MemorySink[] halves = ZoneIslandPaintTest.generate(cx, cz);
            for (int half = 0; half < 2; half++) {
                double[] terrain = plan.densityField(half == 1, false);
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int ly = 0; ly < 128; ly++) {
                            int x = cx * 16 + lx, y = half * 128 + ly, z = cz * 16 + lz;
                            EndBlock b = halves[half].get(x, y, z);
                            if (b == null || isOre(b)) continue;
                            if (DensityField.at(terrain, lx, ly, lz) > 0) continue;
                            if (ArcPaths.density(plan.arcs(), x, y, z) < 2) continue;
                            EndBiome biome = ArcPaths.biomeAt(plan.arcs(), x, y, z);
                            EndBlock expected = biome == null ? EndBlock.STONE : biome.fill;
                            if (biome == null) bare++;
                            else painted++;
                            if (b != expected && wrong++ == 0)
                                first = x + "," + y + "," + z + " " + b + " not " + expected;
                        }
                    }
                }
            }
        }
        assertTrue(painted >= 2000, "only " + painted + " painted tube blocks in " + chunks + " chunks");
        assertTrue(bare >= 2000, "only " + bare + " bare tube blocks in " + chunks + " chunks");
        assertEquals(0, wrong, wrong + " wrong, first " + first);
    }
}

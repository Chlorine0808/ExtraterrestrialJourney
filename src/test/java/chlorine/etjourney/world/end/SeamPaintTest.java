package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Ore;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Around Y 128 the lower half paints and lays ore without seeing the upper half's blocks; faces, sinkholes and
 * canyons all change the rock there. The paint must still count depth from the blocks the upper half really has.
 */
class SeamPaintTest {

    private static final long SEED = 91L;
    private static final Set<EndBlock> ROCK = EnumSet.of(EndBlock.STONE);
    static {
        for (EndBiome b : EndBiome.values()) {
            ROCK.add(b.top);
            ROCK.add(b.fill);
        }
        for (Ore o : Ore.values()) ROCK.add(o.block);
    }

    private static EndBlock get(MemorySink[] halves, int y, int x, int z) {
        if (y < 0 || y > 255) return null;
        return y < 128 ? halves[0].get(x, y, z) : halves[1].get(x, y, z);
    }

    @Test
    void rockAtTheSeamIsPaintedByItsRealDepth() {
        TerrainSampler sampler = ZoneIslandPaintTest.SAMPLER;
        int chunks = 0, checked = 0, wrong = 0;
        String first = "";
        for (int step = 0; step < 60000 && chunks < 80; step++) {
            int cx = (step % 300 - 150) * 3, cz = (step / 300 - 100) * 3;
            double mx = cx * 16 + 8, mz = cz * 16 + 8;
            if (mx * mx + mz * mz < 1200 * 1200 || sampler.land(mx, mz) < 40) continue;
            if (EndBiome.at(SEED, mx, mz) == null) continue;
            ChunkPlan plan = new ChunkPlan(sampler, cx, cz, (a, b) -> Collections.emptyList());
            if (!TallPass.needed(plan)) continue;
            chunks++;
            MemorySink[] halves = ZoneIslandPaintTest.generate(cx, cz);
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    EndBiome biome = EndBiome.at(SEED, x, z);
                    if (biome == null || plan.voidShare(x, z) > 0.5) continue;
                    if (ZoneIslands.ownerAt(plan.zoneIslands(), x, z, 1.6) != null) continue;
                    int depth = 0;
                    for (int y = 160; y >= 118; y--) {
                        EndBlock b = get(halves, y, x, z);
                        // Tubes are made of their own path's fill, and the ground under them is a surface.
                        if (!ROCK.contains(b) || ArcPaintTest.isTube(plan, x, y, z)) {
                            depth = 0;
                            continue;
                        }
                        if (y < 136 && b != EndBlock.STONE && b != biome.top && b != biome.fill) {
                            depth++;
                            continue;
                        }
                        if (y < 136) {
                            EndBlock expected = depth == 0 ? biome.top : depth < 3 ? biome.fill : EndBlock.STONE;
                            checked++;
                            if (b != expected && wrong++ == 0)
                                first = x + "," + y + "," + z + " " + b + " at depth " + depth;
                        }
                        depth++;
                    }
                }
            }
        }
        assertEquals(80, chunks, "only " + chunks + " chunks cross the seam");
        assertTrue(checked > 10000, "only " + checked + " blocks checked");
        assertEquals(0, wrong, wrong + " of " + checked + " wrong, first " + first);
    }
}

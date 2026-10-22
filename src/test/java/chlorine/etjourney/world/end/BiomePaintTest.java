package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The continent and its styles wear the biome of each column; structures and clouds stay bare. */
class BiomePaintTest {

    private static final long SEED = 91L;
    private static final Set<EndBlock> BIOME_BLOCKS = EnumSet.noneOf(EndBlock.class);
    static {
        for (EndBiome b : EndBiome.values()) {
            BIOME_BLOCKS.add(b.top);
            BIOME_BLOCKS.add(b.fill);
        }
    }

    private static EndBlock get(MemorySink[] halves, int x, int y, int z) {
        if (y < 0 || y > 255) return null;
        return y < 128 ? halves[0].get(x, y, z) : halves[1].get(x, y, z);
    }

    private interface Check {

        void column(ChunkPlan plan, MemorySink[] halves, int x, int z);
    }

    /** Runs a check over continent chunks in eight directions, skipping columns near zone islands. */
    private static void overContinent(Check check) {
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4;
            int cx = (int) (Math.cos(angle) * 2400) >> 4, cz = (int) (Math.sin(angle) * 2400) >> 4;
            MemorySink[] halves = ZoneIslandPaintTest.generate(cx, cz);
            ChunkPlan plan = new ChunkPlan(ZoneIslandPaintTest.SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    if (ZoneIslands.ownerAt(plan.zoneIslands(), x, z, 1.6) != null) continue;
                    check.column(plan, halves, x, z);
                }
            }
        }
    }

    @Test
    void groundTopsWearTheirBiome() {
        int[] tops = new int[2];
        overContinent((plan, halves, x, z) -> {
            for (int y = 254; y >= 0; y--) {
                EndBlock b = get(halves, x, y, z);
                if (b == null || get(halves, x, y + 1, z) != null || plan.densityAt(x, y, z) <= 0) continue;
                tops[0]++;
                if (b == EndBiome.at(SEED, x, z).top) tops[1]++;
            }
        });
        assertTrue(tops[0] > 500, "only " + tops[0] + " tops");
        // Sinkholes cut after the paint and may leave a few bare floors.
        assertTrue(tops[1] > tops[0] * 0.97, tops[1] + " of " + tops[0] + " tops painted");
    }

    @Test
    void paintIsOnlySkinDeep() {
        int[] painted = new int[1];
        overContinent((plan, halves, x, z) -> {
            for (int y = 0; y < 256; y++) {
                EndBlock b = get(halves, x, y, z);
                if (!BIOME_BLOCKS.contains(b)) continue;
                painted[0]++;
                boolean open = false;
                for (int up = 1; up <= 3; up++) {
                    EndBlock above = get(halves, x, y + up, z);
                    if (above == null || above == EndBlock.WATER) open = true;
                    else if (!BIOME_BLOCKS.contains(above)) break;
                }
                assertTrue(open, "paint deep inside at " + x + "," + y + "," + z);
            }
        });
        assertTrue(painted[0] > 1000, "only " + painted[0] + " painted");
    }

    /** The two halves of a column agree on where its surface is, so the seam at Y 128 is painted like anywhere. */
    @Test
    void underTheTopComeTwoFills() {
        int[] checked = new int[1];
        overContinent((plan, halves, x, z) -> {
            EndBiome biome = EndBiome.at(SEED, x, z);
            for (int y = 2; y < 256; y++) {
                if (get(halves, x, y, z) != biome.top) continue;
                for (int down = 1; down <= 2; down++) {
                    EndBlock below = get(halves, x, y - down, z);
                    if (below == null) break;
                    checked[0]++;
                    assertEquals(biome.fill, below, "at " + x + "," + (y - down) + "," + z);
                }
            }
        });
        assertTrue(checked[0] > 1000, "only " + checked[0] + " checked");
    }

    @Test
    void cloudsAndStructuresAboveTheGroundStayBare() {
        int above = 0;
        // Walk outwards until enough clouds and structures have been met.
        for (int step = 0; step < 400 && above < 2000; step++) {
            int cx = (1500 + step * 37) >> 4, cz = (step * 53 % 3000 - 1500) >> 4;
            MemorySink[] halves = ZoneIslandPaintTest.generate(cx, cz);
            ChunkPlan plan = new ChunkPlan(ZoneIslandPaintTest.SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        EndBlock b = get(halves, x, y, z);
                        // Rough faces push specks of terrain out a few blocks; those are ground and get painted.
                        if (b == null || plan.densityAt(x, y, z) > -8) continue;
                        above++;
                        assertTrue(!BIOME_BLOCKS.contains(b), "painted structure at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(above >= 2000, "only " + above + " structure blocks above the ground");
    }
}

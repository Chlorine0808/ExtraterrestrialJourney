package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Canyons;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The continent and its styles wear their continent's biome, if any; structures and clouds stay bare. */
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

    /**
     * Runs a check over continent chunks in eight directions, each the first from 2400 out whose centre is continent
     * land with a biome and no void style, skipping columns near zone islands.
     */
    private static void overContinent(Check check) {
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4;
            int cx = 0, cz = 0;
            for (double d = 2400; d < 8000; d += 96) {
                cx = (int) (Math.cos(angle) * d) >> 4;
                cz = (int) (Math.sin(angle) * d) >> 4;
                double mx = cx * 16 + 8, mz = cz * 16 + 8;
                if (EndBiome.at(SEED, mx, mz) == null || ZoneIslandPaintTest.SAMPLER.land(mx, mz) <= 10) continue;
                if (new ChunkPlan(ZoneIslandPaintTest.SAMPLER, cx, cz, (a, b) -> Collections.emptyList())
                    .voidShare(mx, mz) < 0.05) break;
            }
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
                if (ArcPaintTest.isTube(plan, x, y, z)) continue;
                tops[0]++;
                EndBiome biome = EndBiome.at(SEED, x, z);
                if (b == (biome == null ? EndBlock.STONE : biome.top)) tops[1]++;
            }
        });
        assertTrue(tops[0] > 500, "only " + tops[0] + " tops");
        // Sinkholes cut after the paint and may leave a few bare floors.
        assertTrue(tops[1] > tops[0] * 0.97, tops[1] + " of " + tops[0] + " tops painted");
    }

    /** Canyons are cut before the paint, so their floors and walls wear the biome too. */
    @Test
    void canyonFloorsArePainted() {
        TerrainSampler sampler = ZoneIslandPaintTest.SAMPLER;
        int tops = 0, bare = 0;
        String where = "";
        for (int step = 0; step < 40000 && tops < 300; step++) {
            int cx = 80 + (step % 200 - 100) * 8, cz = (step / 200 - 100) * 8;
            double cxm = cx * 16 + 8, czm = cz * 16 + 8;
            if (sampler.styleWeight("SLOT_CANYONS", cxm, czm) < 0.75 || sampler.land(cxm, czm) <= 0) continue;
            ChunkPlan plan = new ChunkPlan(sampler, cx, cz, (a, b) -> Collections.emptyList());
            MemorySink[] halves = ZoneIslandPaintTest.generate(cx, cz);
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    if (Canyons.depth(SEED, x, z) <= 0 || EndBiome.at(SEED, x, z) == null) continue;
                    if (ZoneIslands.ownerAt(plan.zoneIslands(), x, z, 1.6) != null) continue;
                    for (int y = 254; y >= 0; y--) {
                        EndBlock b = get(halves, x, y, z);
                        if (b == null || get(halves, x, y + 1, z) != null) continue;
                        // Clouds and structures placed into the cut later are not ground.
                        if (plan.densityAt(x, y, z) <= 0) continue;
                        tops++;
                        if (!BIOME_BLOCKS.contains(b) && bare++ == 0) where = x + "," + y + "," + z + " " + b;
                    }
                }
            }
        }
        assertTrue(tops >= 300, "only " + tops + " canyon tops");
        assertEquals(0, bare, bare + " of " + tops + " canyon tops bare, first " + where);
    }

    @Test
    void paintIsOnlySkinDeep() {
        int[] painted = new int[1], deep = new int[1];
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
                if (!open && deep[0]++ == 0) System.out.println("paint deep inside at " + x + "," + y + "," + z);
            }
        });
        assertTrue(painted[0] > 1000, "only " + painted[0] + " painted");
        assertEquals(0, deep[0], deep[0] + " of " + painted[0] + " painted deep inside");
    }

    /** The two halves of a column agree on where its surface is, so the seam at Y 128 is painted like anywhere. */
    @Test
    void underTheTopComeTwoFills() {
        int[] checked = new int[1], wrong = new int[1];
        overContinent((plan, halves, x, z) -> {
            EndBiome biome = EndBiome.at(SEED, x, z);
            if (biome == null) return;
            for (int y = 2; y < 256; y++) {
                if (get(halves, x, y, z) != biome.top) continue;
                for (int down = 1; down <= 2; down++) {
                    EndBlock below = get(halves, x, y - down, z);
                    if (below == null) break;
                    checked[0]++;
                    if (below != biome.fill && wrong[0]++ == 0)
                        System.out.println("unfilled at " + x + "," + (y - down) + "," + z + ": " + below);
                }
            }
        });
        assertTrue(checked[0] > 1000, "only " + checked[0] + " checked");
        assertEquals(0, wrong[0], wrong[0] + " of " + checked[0] + " unfilled");
    }

    /** Styles without a continent (arcs, islets, shoals) are left bare, along with what continent rock remains. */
    @Test
    void voidStylesStayBare() {
        int solid = 0;
        for (int step = 0; step < 3000 && solid < 300; step++) {
            int cx = (5000 + step % 60 * 23) >> 4, cz = (step / 60 * 29 - 700) >> 4;
            ChunkPlan plan = new ChunkPlan(ZoneIslandPaintTest.SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            if (plan.voidShare(cx * 16 + 8, cz * 16 + 8) < 0.3) continue;
            MemorySink[] halves = null;
            for (int x = cx * 16; x < cx * 16 + 16; x++) {
                for (int z = cz * 16; z < cz * 16 + 16; z++) {
                    if (plan.voidShare(x, z) <= 0.5 || EndBiome.at(SEED, x, z) == null) continue;
                    if (halves == null) halves = ZoneIslandPaintTest.generate(cx, cz);
                    for (int y = 0; y < 256; y++) {
                        EndBlock b = get(halves, x, y, z);
                        if (b == null || plan.densityAt(x, y, z) <= 0) continue;
                        solid++;
                        assertTrue(!BIOME_BLOCKS.contains(b), "painted void style at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(solid >= 300, "only " + solid + " solid blocks under void styles");
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

package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Ore;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Each ore keeps to its biome or the zone islands and its heights, never shows on the surface, and every one of them
 * can be found.
 */
class OrePlacementTest {

    private static final long SEED = 91L;
    private static final Map<EndBlock, Ore> BY_BLOCK = new EnumMap<>(EndBlock.class);
    static {
        for (Ore ore : Ore.values()) BY_BLOCK.put(ore.block, ore);
    }

    private static final class Sample {

        final int cx, cz;
        final ChunkPlan plan;
        final MemorySink[] halves;

        Sample(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
            plan = new ChunkPlan(ZoneIslandPaintTest.SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            halves = ZoneIslandPaintTest.generate(cx, cz);
        }

        EndBlock get(int x, int y, int z) {
            if (y < 0 || y > 255) return null;
            return y < 128 ? halves[0].get(x, y, z) : halves[1].get(x, y, z);
        }
    }

    private static List<Sample> samples;

    /** Chunks on rings around the centre, wide enough to cross every biome many times. */
    private static synchronized List<Sample> samples() {
        if (samples != null) return samples;
        samples = new ArrayList<>();
        for (int r = 1600; r <= 4000; r += 600) {
            for (int k = 0; k < 24; k++) {
                double angle = k * Math.PI / 12 + r;
                samples.add(new Sample((int) (Math.cos(angle) * r) >> 4, (int) (Math.sin(angle) * r) >> 4));
            }
        }
        return samples;
    }

    private interface OreVisitor {

        void visit(Sample s, Ore ore, int x, int y, int z);
    }

    private static void forEachOre(OreVisitor visitor) {
        for (Sample s : samples()) {
            int ox = s.cx * 16, oz = s.cz * 16;
            for (int x = ox; x < ox + 16; x++) {
                for (int z = oz; z < oz + 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        Ore ore = BY_BLOCK.get(s.get(x, y, z));
                        if (ore != null) visitor.visit(s, ore, x, y, z);
                    }
                }
            }
        }
    }

    @Test
    void everyOreIsFound() {
        Map<Ore, Integer> counts = new EnumMap<>(Ore.class);
        forEachOre((s, ore, x, y, z) -> counts.merge(ore, 1, Integer::sum));
        for (Ore ore : Ore.values()) assertTrue(counts.containsKey(ore), ore + " never found: " + counts);
    }

    @Test
    void oresKeepToTheirBiomeAndHeights() {
        forEachOre((s, ore, x, y, z) -> {
            String at = ore + " at " + x + "," + y + "," + z;
            assertTrue(y >= ore.minY && y <= ore.maxY, at);
            if (ore.biome != null) {
                assertTrue(EndBiome.at(SEED, x, z) == ore.biome, at + " in " + EndBiome.at(SEED, x, z));
                assertTrue(ZoneIslands.ownerAt(s.plan.zoneIslands(), x, z, 1) == null, at + " on a zone island");
            }
            if (ore.onZones) {
                assertTrue(ZoneIslands.ownerAt(s.plan.zoneIslands(), x, z, 1.3) != null, at + " off the zone islands");
            }
        });
    }

    @Test
    void oresNeverShowOnTheSurface() {
        forEachOre((s, ore, x, y, z) -> {
            EndBlock above = s.get(x, y + 1, z);
            assertTrue(above != null && above != EndBlock.WATER, ore + " bare at " + x + "," + y + "," + z);
        });
    }

    /** Ores lie in rock the density grid made, never in clouds or structures drawn block by block. */
    @Test
    void oresLieOnlyInGroundRock() {
        forEachOre(
            (s, ore, x, y, z) -> assertTrue(
                s.plan.densityAt(x, y, z) > -8,
                ore + " away from the ground at " + x + "," + y + "," + z));
    }

    @Test
    void enderGoldIsCommonButNotEverywhere() {
        int[] gold = new int[1], rock = new int[1];
        forEachOre((s, ore, x, y, z) -> { if (ore == Ore.ENDER_GOLD) gold[0]++; });
        for (Sample s : samples()) {
            for (int i = 0; i < s.halves.length; i++) {
                int ox = s.cx * 16, oz = s.cz * 16;
                for (int x = ox; x < ox + 16; x++) for (int z = oz; z < oz + 16; z++)
                    for (int y = i * 128; y < i * 128 + 128; y++) if (s.get(x, y, z) != null) rock[0]++;
            }
        }
        double share = gold[0] / (double) rock[0];
        assertTrue(share > 0.002 && share < 0.03, "ender gold is " + share + " of the rock");
    }
}

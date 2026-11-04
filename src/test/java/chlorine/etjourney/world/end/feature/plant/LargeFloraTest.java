package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * LargeFlora places each biome's large plants in one window. The fake ground has one surface per column; the
 * checks cover which biome a plant stands on, density between biomes, ceilings and the four-chunk limit.
 */
class LargeFloraTest {

    private static final int SURFACE = 60;
    /** How far the foot of a plant may reach past the column it grows from (thick trunks). */
    private static final int OVERHANG = 2;

    /** One surface per column at SURFACE, biome chosen per column, open sky unless a ceiling is given. */
    private static final class Fake implements LargeFlora.Ground {

        final BiFunction<Integer, Integer, EndBiome> biomeAt;
        final int ceiling;

        Fake(BiFunction<Integer, Integer, EndBiome> biomeAt, int ceiling) {
            this.biomeAt = biomeAt;
            this.ceiling = ceiling;
        }

        @Override
        public int surfaces(int x, int z, int[] ys, EndBiome[] biomes) {
            ys[0] = SURFACE;
            biomes[0] = biomeAt.apply(x, z);
            return 1;
        }

        @Override
        public int headroom(int x, int y, int z) {
            return Math.max(0, Math.min(64, ceiling - y));
        }
    }

    private static Fake uniform(EndBiome biome, int ceiling) {
        return new Fake((x, z) -> biome, ceiling);
    }

    private static Voxels window(long seed, int ox, int oz, LargeFlora.Ground ground) {
        Voxels out = new Voxels();
        LargeFlora.plant(new Random(seed), ox, oz, ground, out);
        return out;
    }

    private static Set<EndBlock> blocksOn(EndBiome biome) {
        Set<EndBlock> set = EnumSet.noneOf(EndBlock.class);
        for (long seed = 0; seed < 60; seed++) {
            set.addAll(window(seed, 0, 0, uniform(biome, 255)).cells.values());
        }
        return set;
    }

    @Test
    void everyBiomeHasALargePlant() {
        for (EndBiome biome : EndBiome.values()) {
            boolean found = false;
            for (LargeFlora row : LargeFlora.values()) found |= row.biome == biome;
            assertTrue(found, biome + " has no large plant");
        }
    }

    /** A fallback names another row of the same biome, so valueOf never throws while a window is planted. */
    @Test
    void everyFallbackIsARowOfItsBiome() {
        for (LargeFlora row : LargeFlora.values()) {
            String name = row.fallbackName();
            if (name == null) continue;
            assertEquals(row.biome, LargeFlora.valueOf(name).biome, row + " falls back to " + name);
        }
    }

    @Test
    void standsOnlyOnItsBiome() {
        Set<EndBlock> grove = blocksOn(EndBiome.GROVE), highland = blocksOn(EndBiome.HIGHLAND);
        assertFalse(grove.isEmpty());
        assertFalse(highland.isEmpty());
        for (EndBlock b : grove) assertFalse(highland.contains(b), "block shared by both biomes: " + b);
        Fake checker = new Fake((x, z) -> (((x >> 3) ^ (z >> 3)) & 1) == 0 ? EndBiome.GROVE : EndBiome.HIGHLAND, 255);
        int bottoms = 0;
        for (long seed = 0; seed < 100; seed++) {
            Voxels out = window(seed, 0, 0, checker);
            for (int x = -8; x < 24; x++) {
                for (int z = -8; z < 24; z++) {
                    EndBlock bottom = out.cells.get(Voxels.key(x, SURFACE + 1, z));
                    if (bottom == null) continue;
                    bottoms++;
                    Set<EndBlock> own = grove.contains(bottom) ? grove : highland;
                    EndBiome ownBiome = own == grove ? EndBiome.GROVE : EndBiome.HIGHLAND;
                    // A thick trunk may overhang the next column; the plant's own column never is the wrong biome.
                    boolean near = false;
                    for (int ox = -OVERHANG; ox <= OVERHANG; ox++) {
                        for (int oz = -OVERHANG; oz <= OVERHANG; oz++) {
                            near |= checker.biomeAt.apply(x + ox, z + oz) == ownBiome;
                        }
                    }
                    assertTrue(near, bottom + " stands far from its biome at " + x + "," + z);
                }
            }
        }
        assertTrue(bottoms > 0);
    }

    @Test
    void densityOrder() {
        double[] mean = new double[EndBiome.values().length];
        for (EndBiome biome : EndBiome.values()) {
            long total = 0;
            for (long seed = 0; seed < 200; seed++) total += window(seed, 0, 0, uniform(biome, 255)).cells.size();
            mean[biome.ordinal()] = total / 200.0;
        }
        for (EndBiome biome : EndBiome.values()) {
            if (biome != EndBiome.GROVE) assertTrue(mean[EndBiome.GROVE.ordinal()] > mean[biome.ordinal()], biome + "");
            // AMBER's large plants are loose thin stems now (its trees are placed per cell), so it may be sparser.
            if (biome != EndBiome.HIGHLAND && biome != EndBiome.AMBER) {
                assertTrue(mean[EndBiome.HIGHLAND.ordinal()] < mean[biome.ordinal()], biome + "");
            }
        }
    }

    @Test
    void shrinksOrFallsBackUnderALowCeiling() {
        int ceiling = SURFACE + 1 + 12;
        int written = 0;
        for (long seed = 0; seed < 50; seed++) {
            Voxels out = window(seed, 0, 0, uniform(EndBiome.GROVE, ceiling));
            assertTrue(out.isEmpty() || out.maxY < ceiling, "wrote at or above the ceiling");
            written += out.cells.size();
        }
        assertTrue(written > 0);
    }

    @Test
    void neverWritesOutsideTheFourChunks() {
        for (EndBiome biome : EndBiome.values()) {
            for (long seed = 0; seed < 100; seed++) {
                Voxels out = window(seed, 8, 8, uniform(biome, 255));
                for (long k : out.cells.keySet()) {
                    int x = (int) (k >> 42), z = (int) ((k << 22) >> 42);
                    assertTrue(x >= 0 && x <= 31 && z >= 0 && z <= 31, biome + " wrote at " + x + "," + z);
                }
            }
        }
    }

    @Test
    void sameRandomSameResult() {
        Voxels a = window(7, 0, 0, uniform(EndBiome.GROVE, 255));
        Voxels b = window(7, 0, 0, uniform(EndBiome.GROVE, 255));
        assertFalse(a.isEmpty());
        assertEquals(a.cells, b.cells);
    }

    /** AMBER grows loose tall stems of its own; the seed tree is placed elsewhere, so no seeds here. */
    @Test
    void amberGrowsLooseStems() {
        int stems = 0;
        for (long seed = 0; seed < 200; seed++) {
            for (EndBlock b : window(seed, 0, 0, uniform(EndBiome.AMBER, 255)).cells.values()) {
                assertFalse(b == EndBlock.AMBER_SEED, "seed " + seed + ": a seed from the large flora");
                if (b == EndBlock.AMBER_STEM) stems++;
            }
        }
        assertTrue(stems > 0);
    }

    @Test
    void growsOnlyOnItsOwnBiome() {
        assertTrue(LargeFlora.growsOn(EndBiome.GROVE, EndBiome.GROVE));
        assertFalse(LargeFlora.growsOn(EndBiome.GROVE, EndBiome.HIGHLAND));
    }
}

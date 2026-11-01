package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Plants stand only on biome surfaces inside the populate window, densest in the grove and sparsest on highlands. */
class FloraTest {

    private static final int OX = 40, OZ = -24;

    /** Flat ground of one biome at Y 70 over the whole window. */
    private static Flora.Ground flat(EndBiome biome) {
        return (x, z, ys, biomes) -> {
            ys[0] = 70;
            biomes[0] = biome;
            return 1;
        };
    }

    private static List<int[]> plant(long seed, Flora.Ground ground, Map<Flora, Integer> counts) {
        List<int[]> placed = new ArrayList<>();
        Flora.plant(new Random(seed), OX, OZ, ground, (flora, x, y, z) -> {
            placed.add(new int[] { flora.ordinal(), x, y, z });
            if (counts != null) counts.merge(flora, 1, Integer::sum);
        });
        return placed;
    }

    @Test
    void everyBiomeHasPlantsAndEveryPlantABiome() {
        Set<EndBiome> covered = EnumSet.noneOf(EndBiome.class);
        for (Flora f : Flora.values()) {
            assertTrue(f.biome != null, f.name());
            assertTrue(f.scatter > 0 || f.patches > 0 && f.patchTries > 0, f.name() + " is never placed");
            covered.add(f.biome);
        }
        assertEquals(EnumSet.allOf(EndBiome.class), covered);
    }

    @Test
    void plantsStayInTheWindowOnTheirOwnBiome() {
        // A different biome in each quarter, with a second tier above the first in the north-west.
        Flora.Ground quarters = (x, z, ys, biomes) -> {
            int i = (x - OX < 8 ? 0 : 1) + (z - OZ < 8 ? 0 : 2);
            EndBiome b = EndBiome.values()[i];
            ys[0] = 60;
            biomes[0] = b;
            if (i != 0) return 1;
            ys[1] = 90;
            biomes[1] = b;
            return 2;
        };
        for (long seed = 0; seed < 40; seed++) {
            for (int[] p : plant(seed, quarters, null)) {
                Flora f = Flora.values()[p[0]];
                int x = p[1], y = p[2], z = p[3];
                assertTrue(x >= OX && x < OX + 16 && z >= OZ && z < OZ + 16, "outside the window: " + x + "," + z);
                assertTrue(y == 61 || y == 91, "not on a surface: " + y);
                int i = (x - OX < 8 ? 0 : 1) + (z - OZ < 8 ? 0 : 2);
                assertEquals(EndBiome.values()[i], f.biome, f + " at " + x + "," + z);
            }
        }
    }

    @Test
    void bareGroundAndVoidStayEmpty() {
        Flora.Ground bare = (x, z, ys, biomes) -> 0;
        for (long seed = 0; seed < 20; seed++) assertTrue(plant(seed, bare, null).isEmpty());
    }

    @Test
    void oneSurfaceHoldsOnePlant() {
        for (long seed = 0; seed < 40; seed++) {
            Set<Long> spots = new HashSet<>();
            for (int[] p : plant(seed, flat(EndBiome.GROVE), null)) {
                assertTrue(spots.add(((long) p[1] << 40) ^ ((long) p[2] << 20) ^ p[3] & 0xFFFFF), "twice at a spot");
            }
        }
    }

    @Test
    void layeredColumnsGetPlantsOnEveryTier() {
        Flora.Ground tiers = (x, z, ys, biomes) -> {
            ys[0] = 100;
            ys[1] = 60;
            biomes[0] = biomes[1] = EndBiome.GROVE;
            return 2;
        };
        boolean upper = false, lower = false;
        for (int[] p : plant(3, tiers, null)) {
            upper |= p[2] == 101;
            lower |= p[2] == 61;
        }
        assertTrue(upper && lower);
    }

    @Test
    void sameRandomGivesTheSamePlants() {
        List<int[]> a = plant(9, flat(EndBiome.MYCELIA), null), b = plant(9, flat(EndBiome.MYCELIA), null);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) for (int k = 0; k < 4; k++) assertEquals(a.get(i)[k], b.get(i)[k]);
    }

    @Test
    void groveIsDensestAndHighlandSparsest() {
        Map<EndBiome, Integer> perWindow = new EnumMap<>(EndBiome.class);
        int windows = 200;
        for (EndBiome biome : EndBiome.values()) {
            int total = 0;
            for (long seed = 0; seed < windows; seed++) total += plant(seed, flat(biome), null).size();
            perWindow.put(biome, total / windows);
        }
        // 20% + 4% of 256 surfaces plus about 1.5 patches of flowers.
        int grove = perWindow.get(EndBiome.GROVE);
        assertTrue(grove > 55 && grove < 80, "grove " + grove);
        for (EndBiome biome : EndBiome.values()) {
            assertTrue(perWindow.get(biome) <= grove, biome + " " + perWindow);
            assertTrue(perWindow.get(biome) >= perWindow.get(EndBiome.HIGHLAND), biome + " " + perWindow);
        }
        assertTrue(perWindow.get(EndBiome.HIGHLAND) > 5, "highland " + perWindow);
    }

    /** A column from a map of Y to biome surface; any Y listed in solid is a block that is neither air nor surface. */
    private static Flora.Column column(int top, Map<Integer, EndBiome> surfaces, Set<Integer> solid) {
        return new Flora.Column() {

            @Override
            public int top() {
                return top;
            }

            @Override
            public EndBiome surfaceAt(int y) {
                return surfaces.get(y);
            }

            @Override
            public boolean isAir(int y) {
                return !surfaces.containsKey(y) && !solid.contains(y);
            }
        };
    }

    @Test
    void scanFindsEveryOpenSurfaceTopFirst() {
        Map<Integer, EndBiome> surfaces = new HashMap<>();
        surfaces.put(120, EndBiome.GROVE);
        surfaces.put(80, EndBiome.AMBER);
        surfaces.put(40, EndBiome.GROVE);
        // Water over the lowest surface: no plant there.
        Set<Integer> solid = new HashSet<>();
        solid.add(41);
        int[] ys = new int[Flora.MAX_SURFACES];
        EndBiome[] biomes = new EndBiome[Flora.MAX_SURFACES];
        int n = Flora.scan(column(200, surfaces, solid), ys, biomes);
        assertEquals(2, n);
        assertEquals(120, ys[0]);
        assertEquals(EndBiome.GROVE, biomes[0]);
        assertEquals(80, ys[1]);
        assertEquals(EndBiome.AMBER, biomes[1]);
    }

    @Test
    void scanSkipsTheBuildLimitAndStopsWhenFull() {
        Map<Integer, EndBiome> surfaces = new HashMap<>();
        surfaces.put(255, EndBiome.GROVE);
        for (int y = 0; y < 200; y += 2) surfaces.put(y, EndBiome.MYCELIA);
        int[] ys = new int[Flora.MAX_SURFACES];
        EndBiome[] biomes = new EndBiome[Flora.MAX_SURFACES];
        int n = Flora.scan(column(255, surfaces, new HashSet<>()), ys, biomes);
        assertEquals(Flora.MAX_SURFACES, n);
        assertEquals(198, ys[0]);
    }

    @Test
    void scanOfAnEmptyColumnFindsNothing() {
        int[] ys = new int[Flora.MAX_SURFACES];
        EndBiome[] biomes = new EndBiome[Flora.MAX_SURFACES];
        assertEquals(0, Flora.scan(column(-1, new HashMap<>(), new HashSet<>()), ys, biomes));
    }

    @Test
    void patchPlantsGrowInClumps() {
        Map<Flora, Integer> counts = new EnumMap<>(Flora.class);
        int windows = 400, clumped = 0, found = 0;
        for (long seed = 0; seed < windows; seed++) {
            List<int[]> blooms = new ArrayList<>();
            for (int[] p : plant(seed, flat(EndBiome.GROVE), counts)) {
                if (p[0] == Flora.GROVE_BLOOM.ordinal()) blooms.add(p);
            }
            for (int[] p : blooms) {
                found++;
                for (int[] q : blooms) {
                    if (q != p && Math.abs(q[1] - p[1]) <= 2 && Math.abs(q[3] - p[3]) <= 2) {
                        clumped++;
                        break;
                    }
                }
            }
        }
        assertTrue(found > 0);
        assertTrue(clumped > found * 3 / 4, clumped + " of " + found + " blooms have a neighbour");
        assertFalse(counts.containsKey(Flora.MYCELIA_SMALLSHROOM), "a mycelia plant in the grove");
    }
}

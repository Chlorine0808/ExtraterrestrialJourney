package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The HIGHLAND crystal cluster: leaning crystal pillars with budding crystals inside and buds on top. */
class CrystalClusterTest {

    private static final int X = 100, Y = 64, Z = -37;
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static boolean isCrystal(EndBlock b) {
        return b == EndBlock.CRYSTAL || b == EndBlock.BUDDING_CRYSTAL;
    }

    @Test
    void staysWithinEight() {
        assertTrue(CrystalCluster.INSTANCE.radius() <= 8);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v), "seed " + seed);
            assertTrue(v.maxDistance(X, Z) <= CrystalCluster.INSTANCE.radius(), "seed " + seed);
            assertTrue(v.minY >= Y, "seed " + seed);
        }
    }

    @Test
    void staysBelowThirty() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 60, v);
            assertTrue(v.height(Y) <= 30, "seed " + seed);
        }
    }

    @Test
    void buddingIsEnclosedByCrystal() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            int budding = v.count(EndBlock.BUDDING_CRYSTAL);
            assertTrue(budding >= 1 && budding <= 3, "seed " + seed);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = 0; dy <= 31; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        if (v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz)) != EndBlock.BUDDING_CRYSTAL) continue;
                        int shut = 0;
                        for (int[] f : FACES) {
                            if (isCrystal(v.cells.get(Voxels.key(X + dx + f[0], Y + dy + f[1], Z + dz + f[2])))) {
                                shut++;
                            }
                        }
                        assertTrue(shut >= 4, "seed " + seed + " has " + shut + " crystal faces");
                    }
                }
            }
        }
    }

    @Test
    void budsSitOnCrystal() {
        Set<EndBlock> buds = EnumSet.of(
            EndBlock.CRYSTAL_BUD_SMALL,
            EndBlock.CRYSTAL_BUD_MEDIUM,
            EndBlock.CRYSTAL_BUD_LARGE,
            EndBlock.CRYSTAL_CLUSTER);
        int seen = 0;
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = 0; dy <= 31; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        if (!buds.contains(v.cells.get(Voxels.key(X + dx, Y + dy, Z + dz)))) continue;
                        seen++;
                        assertTrue(
                            isCrystal(v.cells.get(Voxels.key(X + dx, Y + dy - 1, Z + dz))),
                            "seed " + seed + " bud at " + dx + "," + dy + "," + dz);
                    }
                }
            }
        }
        assertTrue(seen > 0);
    }

    @Test
    void usesAllFourStages() {
        Set<EndBlock> seen = EnumSet.noneOf(EndBlock.class);
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            for (EndBlock b : v.cells.values()) {
                if (b.name()
                    .startsWith("CRYSTAL_")) seen.add(b);
            }
        }
        assertEquals(
            EnumSet.of(
                EndBlock.CRYSTAL_BUD_SMALL,
                EndBlock.CRYSTAL_BUD_MEDIUM,
                EndBlock.CRYSTAL_BUD_LARGE,
                EndBlock.CRYSTAL_CLUSTER),
            seen);
    }

    @Test
    void pillarsReachUpAndLean() {
        boolean leaned = false;
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 40, v);
            assertTrue(v.count(EndBlock.CRYSTAL) >= 15, "seed " + seed);
            assertTrue(v.height(Y) >= 5, "seed " + seed);
            if (v.maxDistance(X, Z) >= 4) leaned = true;
        }
        assertTrue(leaned);
    }

    @Test
    void shrinksToTheHeadroom() {
        for (int seed = 0; seed < 100; seed++) {
            Voxels v = new Voxels();
            assertTrue(CrystalCluster.INSTANCE.grow(new Random(seed), X, Y, Z, 6, v), "seed " + seed);
            assertTrue(v.height(Y) <= 5, "seed " + seed);
        }
    }

    @Test
    void refusesWhenTooLow() {
        Voxels v = new Voxels();
        assertFalse(CrystalCluster.INSTANCE.grow(new Random(1), X, Y, Z, 5, v));
        assertTrue(v.isEmpty());
    }
}

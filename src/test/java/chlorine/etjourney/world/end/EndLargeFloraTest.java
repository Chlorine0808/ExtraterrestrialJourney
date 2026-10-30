package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Flora;
import chlorine.etjourney.world.end.feature.plant.LargeFlora;

/** The glue of EndLargeFlora: the four-chunk write check, and the Random it draws from. */
class EndLargeFloraTest {

    private static final int SURFACE = 60;

    @Test
    void readsAndWritesOnlyTheFourChunks() {
        // Chunk (2, 3) owns x 32..63 and z 48..79.
        assertTrue(EndLargeFlora.writable(2, 3, 32, 60, 48));
        assertTrue(EndLargeFlora.writable(2, 3, 63, 60, 79));
        assertFalse(EndLargeFlora.writable(2, 3, 31, 60, 48));
        assertFalse(EndLargeFlora.writable(2, 3, 64, 60, 48));
        assertFalse(EndLargeFlora.writable(2, 3, 32, 60, 47));
        assertFalse(EndLargeFlora.writable(2, 3, 32, 60, 80));
        assertTrue(EndLargeFlora.writable(2, 3, 40, 1, 50));
        assertTrue(EndLargeFlora.writable(2, 3, 40, 255, 50));
        assertFalse(EndLargeFlora.writable(2, 3, 40, 0, 50));
        assertFalse(EndLargeFlora.writable(2, 3, 40, 256, 50));
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) ^ (((long) z & 0xFFFFFL) << 20) ^ y;
    }

    /**
     * Blooms set in one flat GROVE window seeded with seed, after the large plants when large is true. FML seeds
     * every generator's Random alike, so Flora gets new Random(seed) and the large plants what ownRandom makes of
     * one. A column whose surface a plant covers is no surface for Flora, and Flora never writes over a plant.
     */
    private static int blooms(long seed, boolean large) {
        Set<Long> taken = new HashSet<>();
        if (large) {
            LargeFlora.Ground ground = new LargeFlora.Ground() {

                @Override
                public int surfaces(int x, int z, int[] ys, EndBiome[] biomes) {
                    ys[0] = SURFACE;
                    biomes[0] = EndBiome.GROVE;
                    return 1;
                }

                @Override
                public int headroom(int x, int y, int z) {
                    return 64;
                }
            };
            LargeFlora.plant(
                EndLargeFlora.ownRandom(new Random(seed)),
                0,
                0,
                ground,
                (x, y, z, block) -> taken.add(key(x, y, z)));
        }
        int[] count = new int[1];
        Flora.plant(new Random(seed), 0, 0, (x, z, ys, biomes) -> {
            if (taken.contains(key(x, SURFACE + 1, z))) return 0;
            ys[0] = SURFACE;
            biomes[0] = EndBiome.GROVE;
            return 1;
        }, (flora, x, y, z) -> { if (flora == Flora.GROVE_BLOOM && !taken.contains(key(x, y, z))) count[0]++; });
        return count[0];
    }

    /**
     * Large plants must not wipe out the small flora at their feet. Trunks and shrubs cover some ground, so a few
     * blooms go; but with a Random shared with Flora the first tree lands on the first bloom patch and about two
     * thirds of the blooms vanish. Keeping 80% leaves room for the ground the plants really cover.
     */
    @Test
    void smallFloraStillLandInWindowsWithLargePlants() {
        long without = 0, with = 0;
        for (long seed = 0; seed < 400; seed++) {
            without += blooms(seed, false);
            with += blooms(seed, true);
        }
        System.out.println("blooms in 400 GROVE windows: " + without + " alone, " + with + " with large plants");
        assertTrue(with >= 0.8 * without, with + " of " + without + " blooms left");
    }
}

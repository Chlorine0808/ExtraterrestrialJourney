package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Slot canyons are narrow, cut 30-60 deep, and cover a modest share of the ground. */
class CanyonsTest {

    private static final long SEED = 42L;

    @Test
    void depthStaysWithinRange() {
        for (int i = 0; i < 20000; i++) {
            double d = Canyons.depth(SEED, i * 3.7, i * 1.3);
            assertTrue(d == 0 || (d >= Canyons.MIN_DEPTH * 0.75 && d <= Canyons.MAX_DEPTH), "depth " + d);
        }
    }

    @Test
    void slotsAreNarrow() {
        int slots = 0, wide = 0, run = 0;
        for (int line = 0; line < 40; line++) {
            for (int x = 0; x < 4000; x++) {
                if (Canyons.depth(SEED, x, line * 97) > 0) {
                    run++;
                    continue;
                }
                if (run > 0) {
                    slots++;
                    // Crossing a slot at a slant widens it; a straight crossing is 3-6 blocks.
                    if (run > 14) wide++;
                }
                run = 0;
            }
        }
        assertTrue(slots > 100, "slots " + slots);
        assertTrue(wide < slots / 10, wide + " of " + slots + " crossings were wide");
    }

    @Test
    void slotsCoverAModestShare() {
        int cut = 0, total = 0;
        for (int x = 0; x < 600; x += 2) {
            for (int z = 0; z < 600; z += 2) {
                total++;
                if (Canyons.depth(SEED, x, z) > 0) cut++;
            }
        }
        double share = cut / (double) total;
        assertTrue(share > 0.03 && share < 0.25, "share " + share);
    }
}

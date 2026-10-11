package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The ball cut ahead of the players is the wanted one nearest to a player, within reach. */
class BiospherePrefetchTest {

    private static final long SEED = 41L;
    private static final double REACH = 300;
    private static final StructureProbe VOID = new StructureProbe() {

        @Override
        public double weight(String style, double x, double z) {
            return 1;
        }

        @Override
        public double land(double x, double z) {
            return -100;
        }

        @Override
        public double ground(double x, double z) {
            return -1000;
        }

        @Override
        public double underside(double x, double z) {
            return -1000;
        }
    };

    private static double distance(Biosphere b, double[] p) {
        return Math.hypot(b.centreX - p[0], b.centreZ - p[1]);
    }

    @Test
    void picksTheNearestBallWithinReach() {
        double[] player = { 5000, -3000 };
        Biosphere b = BiospherePrefetch.pick(SEED, Collections.singletonList(player), REACH, VOID, x -> true);
        assertNotNull(b);
        for (Biosphere other : Biosphere.KIND.near(SEED, player[0], player[1], REACH, VOID)) {
            assertTrue(distance(b, player) <= distance(other, player));
        }
        assertTrue(distance(b, player) <= REACH + b.footprint);
    }

    @Test
    void skipsBallsThatAreNotWanted() {
        double[] player = { 5000, -3000 };
        List<double[]> players = Collections.singletonList(player);
        Biosphere first = BiospherePrefetch.pick(SEED, players, REACH, VOID, x -> true);
        Biosphere second = BiospherePrefetch.pick(SEED, players, REACH, VOID, x -> x.key() != first.key());
        assertNotNull(second);
        assertTrue(second.key() != first.key());
        assertTrue(distance(second, player) >= distance(first, player));
        assertNull(BiospherePrefetch.pick(SEED, players, REACH, VOID, x -> false));
    }

    @Test
    void choosesAcrossPlayers() {
        double[] far = { 5000, -3000 }, near = { -7000, 4000 };
        Biosphere a = BiospherePrefetch.pick(SEED, Collections.singletonList(far), REACH, VOID, x -> true);
        Biosphere b = BiospherePrefetch.pick(SEED, Collections.singletonList(near), REACH, VOID, x -> true);
        Biosphere both = BiospherePrefetch.pick(SEED, Arrays.asList(far, near), REACH, VOID, x -> true);
        Biosphere expected = distance(a, far) <= distance(b, near) ? a : b;
        assertEquals(expected.key(), both.key());
    }
}

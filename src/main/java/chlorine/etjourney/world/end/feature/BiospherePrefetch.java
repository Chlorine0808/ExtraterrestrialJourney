package chlorine.etjourney.world.end.feature;

import java.util.List;
import java.util.function.Predicate;

import chlorine.etjourney.world.end.noise.Distance;

/** Which biosphere to cut ahead of the players. */
public final class BiospherePrefetch {

    private BiospherePrefetch() {}

    /** Of the wanted balls within reach of a player, {x, z}, the one nearest to its player; null when none. */
    public static Biosphere pick(long seed, List<double[]> players, double reach, StructureProbe probe,
        Predicate<Biosphere> wanted) {
        Biosphere best = null;
        double bestDistance = Double.MAX_VALUE;
        for (double[] p : players) {
            for (Biosphere b : Biosphere.KIND.near(seed, p[0], p[1], reach, probe)) {
                double d = Distance.planar(b.centreX - p[0], b.centreZ - p[1]);
                if (d >= bestDistance || !wanted.test(b)) continue;
                best = b;
                bestDistance = d;
            }
        }
        return best;
    }
}

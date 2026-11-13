package chlorine.etjourney.core.growth.model;

import java.util.Random;

/** Draws a rarity in proportion to its weight. */
public final class RarityTable {

    private RarityTable() {}

    /** The rarity at u, a uniform value in [0, 1), along the weights laid end to end. */
    public static Rarity pick(double u) {
        int total = 0;
        for (Rarity r : Rarity.values()) total += r.weight;
        double at = u * total;
        for (Rarity r : Rarity.values()) {
            if (at < r.weight) return r;
            at -= r.weight;
        }
        return Rarity.values()[Rarity.values().length - 1];
    }

    public static Rarity roll(Random random) {
        return pick(random.nextDouble());
    }
}

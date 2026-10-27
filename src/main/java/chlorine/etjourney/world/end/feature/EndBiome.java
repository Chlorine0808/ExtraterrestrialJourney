package chlorine.etjourney.world.end.feature;

import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;

/** Surface looks of the continents: each continent draws one biome, or none and stays bare end stone. */
public enum EndBiome {

    GROVE(EndBlock.GROVE_TOP, EndBlock.GROVE_FILL),
    HIGHLAND(EndBlock.HIGHLAND_TOP, EndBlock.HIGHLAND_FILL),
    MYCELIA(EndBlock.MYCELIA_TOP, EndBlock.MYCELIA_FILL),
    AMBER(EndBlock.AMBER_TOP, EndBlock.AMBER_FILL);

    private static final double BARE_SHARE = 0.5;
    private static final long SALT = 0x5D1E7A3C29B4F086L;
    private static final EndBiome[] ALL = values();

    public final EndBlock top, fill;

    EndBiome(EndBlock top, EndBlock fill) {
        this.top = top;
        this.fill = fill;
    }

    /** The biome of the continent whose land rises highest at (x, z), or null for bare ground. */
    public static EndBiome at(long seed, List<Continent.Seed> seeds, double x, double z) {
        double[] w = Continent.warped(seed, x, z);
        Continent.Seed owner = null;
        double best = Double.NEGATIVE_INFINITY;
        for (Continent.Seed s : seeds) {
            double h = 100 - Distance.planar(w[0] - s.x, w[1] - s.z) / 8 * s.falloff;
            if (h > best) {
                best = h;
                owner = s;
            }
        }
        return owner == null ? null : ofContinent(seed, ContinentIds.of(seed, owner));
    }

    /** Convenience for one column: looks up the seeds itself. */
    public static EndBiome at(long seed, double x, double z) {
        return at(seed, Continent.seedsNear(seed, x, z, Continent.warpReach()), x, z);
    }

    static EndBiome ofContinent(long seed, long id) {
        double roll = Hash.hash01(seed ^ SALT, (int) (id >> 32), (int) id);
        if (roll < BARE_SHARE) return null;
        return ALL[Math.min(ALL.length - 1, (int) ((roll - BARE_SHARE) / (1 - BARE_SHARE) * ALL.length))];
    }
}

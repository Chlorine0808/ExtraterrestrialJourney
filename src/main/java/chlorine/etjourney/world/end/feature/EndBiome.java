package chlorine.etjourney.world.end.feature;

import java.util.List;

import chlorine.etjourney.world.end.modifier.EndBlock;
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

    /**
     * The biome of the continent whose land rises highest at (x, z), or null for bare ground. Where a mountain's feet
     * rise higher, the column belongs to the continent the mountain stands on.
     */
    public static EndBiome at(long seed, List<Continent.Seed> seeds, List<Mountains.Mountain> mountains, double x,
        double z) {
        double[] w = Continent.warped(seed, x, z);
        return atWarped(seed, seeds, mountains, w[0], w[1]);
    }

    /** Convenience for one column: looks up the seeds and mountains itself. */
    public static EndBiome at(long seed, double x, double z) {
        return at(seed, Continent.seedsNear(seed, x, z, Continent.warpReach()), Mountains.near(seed, x, z, 0), x, z);
    }

    static EndBiome atWarped(long seed, List<Continent.Seed> seeds, List<Mountains.Mountain> mountains, double wx,
        double wz) {
        Continent.Seed owner = Continent.highest(seeds, wx, wz);
        double best = Continent.rawHeight(seeds, wx, wz);
        for (Mountains.Mountain m : mountains) {
            double foot = Mountains.footLand(m, wx, wz);
            if (foot > best && m.anchor != null) {
                best = foot;
                owner = m.anchor;
            }
        }
        return owner == null ? null : ofContinent(seed, ContinentIds.of(seed, owner));
    }

    static EndBiome ofContinent(long seed, long id) {
        double roll = Hash.hash01(seed ^ SALT, (int) (id >> 32), (int) id);
        if (roll < BARE_SHARE) return null;
        return ALL[Math.min(ALL.length - 1, (int) ((roll - BARE_SHARE) / (1 - BARE_SHARE) * ALL.length))];
    }
}

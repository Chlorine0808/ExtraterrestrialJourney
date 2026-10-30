package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Flora;

/** The large plants of each biome and where one populate window puts them; rows run big before small. */
public enum LargeFlora {

    GROVE_TREE(EndBiome.GROVE, GroveTree.INSTANCE, 2.5, 1, "GROVE_SHRUB"),
    GROVE_SHRUB(EndBiome.GROVE, GroveShrub.INSTANCE, 2.0, 3, null),
    MYCELIA_LARGE(EndBiome.MYCELIA, GiantMushroom.LARGE, 0.4, 1, "MYCELIA_MEDIUM"),
    MYCELIA_MEDIUM(EndBiome.MYCELIA, GiantMushroom.MEDIUM, 0.8, 1, "MYCELIA_SMALL"),
    MYCELIA_SMALL(EndBiome.MYCELIA, GiantMushroom.SMALL, 1.0, 2, null),
    AMBER_LIGHTROOT(EndBiome.AMBER, Lightroot.INSTANCE, 0.6, 1, null),
    HIGHLAND_CRYSTAL(EndBiome.HIGHLAND, CrystalCluster.INSTANCE, 0.3, 1, null),
    HIGHLAND_TREE(EndBiome.HIGHLAND, TwistedTree.INSTANCE, 0.2, 1, null);

    /** How far a cluster try may lie from its patch centre, in x and z. */
    private static final int SPREAD = 3;
    /** Tall stems tried around each lightroot, how far from its centre, and how far up or down from its ground. */
    private static final int STEM_TRIES = 6;
    private static final int STEM_AROUND = 6;
    private static final int STEM_RISE = 3;
    private static final int WINDOW = Flora.WINDOW;
    private static final LargeFlora[] ALL = values();

    public final EndBiome biome;
    public final Grower grower;
    /** Expected patches per window, and tries per patch. */
    public final double patches;
    public final int tries;
    private final String fallback;

    LargeFlora(EndBiome biome, Grower grower, double patches, int tries, String fallback) {
        this.biome = biome;
        this.grower = grower;
        this.patches = patches;
        this.tries = tries;
        this.fallback = fallback;
    }

    public interface Ground {

        /** Same contract as Flora.Ground, but any column of the four chunks may be asked. */
        int surfaces(int x, int z, int[] ys, EndBiome[] biomes);

        /** Air blocks from (x, y, z) upwards, counted up to 64. */
        int headroom(int x, int y, int z);
    }

    /** Plants the window at (originX, originZ), returning how many plants grew; writes stay within 8 blocks. */
    public static int plant(Random random, int originX, int originZ, Ground ground, PlantSink out) {
        int[] ys = new int[Flora.MAX_SURFACES];
        EndBiome[] biomes = new EndBiome[Flora.MAX_SURFACES];
        int grown = 0;
        for (LargeFlora row : ALL) {
            int count = (int) row.patches + (random.nextDouble() < row.patches % 1 ? 1 : 0);
            for (int i = 0; i < count; i++) grown += row.patch(random, originX, originZ, ground, ys, biomes, out);
        }
        return grown;
    }

    /** Whether a plant of this biome may grow on a surface of that biome (saplings, bone meal). */
    public static boolean growsOn(EndBiome own, EndBiome under) {
        return own == under;
    }

    private int patch(Random random, int originX, int originZ, Ground ground, int[] ys, EndBiome[] biomes,
        PlantSink out) {
        int cx = random.nextInt(WINDOW), cz = random.nextInt(WINDOW);
        if (!hasSurface(random, originX + cx, originZ + cz, ground, ys, biomes)) return 0;
        int grown = 0;
        for (int t = 0; t < tries; t++) {
            int dx = tries == 1 ? cx : cx + random.nextInt(2 * SPREAD + 1) - SPREAD;
            int dz = tries == 1 ? cz : cz + random.nextInt(2 * SPREAD + 1) - SPREAD;
            if (dx < 0 || dz < 0 || dx >= WINDOW || dz >= WINDOW) continue;
            int x = originX + dx, z = originZ + dz;
            if (!hasSurface(random, x, z, ground, ys, biomes)) continue;
            int y = ys[0] + 1;
            int headroom = ground.headroom(x, y, z);
            for (LargeFlora row = this; row != null; row = row.fallback()) {
                if (row.grower.grow(random, x, y, z, headroom, out)) {
                    grown++;
                    if (row == AMBER_LIGHTROOT)
                        grown += stems(random, originX, originZ, dx, dz, y - 1, ground, ys, biomes, out);
                    break;
                }
            }
        }
        return grown;
    }

    /** Tall stems around the lightroot at window offset (cx, cz) on ground ground; returns how many grew. */
    private static int stems(Random random, int originX, int originZ, int cx, int cz, int ground, Ground in, int[] ys,
        EndBiome[] biomes, PlantSink out) {
        int grown = 0;
        for (int t = 0; t < STEM_TRIES; t++) {
            int ox = random.nextInt(2 * STEM_AROUND + 1) - STEM_AROUND;
            int oz = random.nextInt(2 * STEM_AROUND + 1) - STEM_AROUND;
            // The stalk and the seed ball stand at the centre.
            if (Math.abs(ox) <= 1 && Math.abs(oz) <= 1) continue;
            int dx = cx + ox, dz = cz + oz;
            if (dx < 0 || dz < 0 || dx >= WINDOW || dz >= WINDOW) continue;
            int x = originX + dx, z = originZ + dz;
            int y = nearest(in.surfaces(x, z, ys, biomes), ys, biomes, ground);
            if (y < 0) continue;
            if (TallStem.INSTANCE.grow(random, x, y + 1, z, in.headroom(x, y + 1, z), out)) grown++;
        }
        return grown;
    }

    /** The AMBER surface within STEM_RISE of ground in height, nearest it, or -1. */
    private static int nearest(int n, int[] ys, EndBiome[] biomes, int ground) {
        int best = -1;
        for (int s = 0; s < Math.min(n, Flora.MAX_SURFACES); s++) {
            int off = Math.abs(ys[s] - ground);
            if (biomes[s] != EndBiome.AMBER || ys[s] < 0 || ys[s] >= 255 || off > STEM_RISE) continue;
            if (best < 0 || off < Math.abs(best - ground)) best = ys[s];
        }
        return best;
    }

    /** Puts a random surface of this biome in the column at ys[0] (the Flora.pick idea); false if there is none. */
    private boolean hasSurface(Random random, int x, int z, Ground ground, int[] ys, EndBiome[] biomes) {
        int n = Math.min(Flora.MAX_SURFACES, ground.surfaces(x, z, ys, biomes));
        int found = 0, y = -1;
        for (int s = 0; s < n; s++) {
            // A surface at the build limit has no room above for a plant.
            if (biomes[s] == biome && ys[s] >= 0 && ys[s] < 255 && random.nextInt(++found) == 0) y = ys[s];
        }
        ys[0] = y;
        return y >= 0;
    }

    /** The name of the row tried when this one does not fit, or null. */
    String fallbackName() {
        return fallback;
    }

    private LargeFlora fallback() {
        return fallback == null ? null : valueOf(fallback);
    }
}

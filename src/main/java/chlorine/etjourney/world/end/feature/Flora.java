package chlorine.etjourney.world.end.feature;

import java.util.BitSet;
import java.util.Random;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Grass, flowers and mushrooms of the biome surfaces, set at populate time within one 16x16 window. */
public enum Flora {

    GROVE_GRASS(EndBlock.GROVE_GRASS, EndBiome.GROVE, 0.20, 0, 0),
    GROVE_FERN(EndBlock.GROVE_FERN, EndBiome.GROVE, 0.04, 0, 0),
    GROVE_BLOOM(EndBlock.GROVE_BLOOM, EndBiome.GROVE, 0, 1.5, 8),
    HIGHLAND_TUFT(EndBlock.HIGHLAND_TUFT, EndBiome.HIGHLAND, 0.05, 0, 0),
    HIGHLAND_THISTLE(EndBlock.HIGHLAND_THISTLE, EndBiome.HIGHLAND, 0, 0.5, 6),
    MYCELIA_SPROUT(EndBlock.MYCELIA_SPROUT, EndBiome.MYCELIA, 0.12, 0, 0),
    MYCELIA_SMALLSHROOM(EndBlock.MYCELIA_SMALLSHROOM, EndBiome.MYCELIA, 0, 1.5, 6),
    AMBER_REED(EndBlock.AMBER_REED, EndBiome.AMBER, 0.06, 0, 0),
    AMBER_BULB(EndBlock.AMBER_BULB, EndBiome.AMBER, 0, 0.5, 5);

    public static final int WINDOW = 16;
    /** Most surfaces one column may report. */
    public static final int MAX_SURFACES = 32;
    /** How far a patch reaches from its centre, across and up or down. */
    private static final int SPREAD = 3;
    private static final Flora[] ALL = values();

    public final EndBlock block;
    public final EndBiome biome;
    /** Chance to stand on each free surface of its biome. */
    public final double scatter;
    /** Expected patches per window, and placement tries per patch. */
    public final double patches;
    public final int patchTries;

    Flora(EndBlock block, EndBiome biome, double scatter, double patches, int patchTries) {
        this.block = block;
        this.biome = biome;
        this.scatter = scatter;
        this.patches = patches;
        this.patchTries = patchTries;
    }

    /** The biome surfaces of a column: Y of each surface block with air above it, top first. */
    public interface Ground {

        /** Fills ys and biomes (MAX_SURFACES long) and returns how many surfaces there are. */
        int surfaces(int x, int z, int[] ys, EndBiome[] biomes);
    }

    /** The blocks of one world column, as far as finding biome surfaces needs. */
    public interface Column {

        /** Highest Y that may hold a block, or below 0 for an empty column. */
        int top();

        /** The biome whose surface block lies at y, or null. */
        EndBiome surfaceAt(int y);

        boolean isAir(int y);
    }

    /** Collects a column's biome surfaces with air above, top first, into ys and biomes; returns how many. */
    public static int scan(Column column, int[] ys, EndBiome[] biomes) {
        int n = 0;
        // Y 255 is skipped: a plant needs the block above it.
        for (int y = Math.min(254, column.top()); y >= 0 && n < ys.length && n < biomes.length; y--) {
            EndBiome biome = column.surfaceAt(y);
            if (biome == null || !column.isAir(y + 1)) continue;
            ys[n] = y;
            biomes[n++] = biome;
        }
        return n;
    }

    public interface Planter {

        /** Sets a plant at (x, y, z), the air block just above its surface. */
        void plant(Flora flora, int x, int y, int z);
    }

    /** Plants the window at (originX, originZ): patches first, then scattered plants on the surfaces left free. */
    public static void plant(Random random, int originX, int originZ, Ground ground, Planter out) {
        int[] ys = new int[MAX_SURFACES];
        EndBiome[] biomes = new EndBiome[MAX_SURFACES];
        BitSet taken = new BitSet(WINDOW * WINDOW * 256);
        for (Flora flora : ALL) {
            if (flora.patches <= 0) continue;
            int count = (int) flora.patches + (random.nextDouble() < flora.patches % 1 ? 1 : 0);
            for (int i = 0; i < count; i++) flora.patch(random, originX, originZ, ground, ys, biomes, taken, out);
        }
        for (int dx = 0; dx < WINDOW; dx++) {
            for (int dz = 0; dz < WINDOW; dz++) {
                int x = originX + dx, z = originZ + dz;
                int n = surfaces(ground, x, z, ys, biomes);
                for (int s = 0; s < n; s++) {
                    if (biomes[s] == null || taken.get(spot(dx, dz, ys[s]))) continue;
                    for (Flora flora : ALL) {
                        if (flora.biome != biomes[s] || flora.scatter <= 0 || random.nextDouble() >= flora.scatter) {
                            continue;
                        }
                        taken.set(spot(dx, dz, ys[s]));
                        out.plant(flora, x, ys[s] + 1, z);
                        break;
                    }
                }
            }
        }
    }

    private void patch(Random random, int originX, int originZ, Ground ground, int[] ys, EndBiome[] biomes,
        BitSet taken, Planter out) {
        int cx = random.nextInt(WINDOW), cz = random.nextInt(WINDOW);
        int centreY = pick(random, surfaces(ground, originX + cx, originZ + cz, ys, biomes), ys, biomes);
        if (centreY < 0) return;
        for (int t = 0; t < patchTries; t++) {
            int dx = cx + random.nextInt(2 * SPREAD + 1) - SPREAD, dz = cz + random.nextInt(2 * SPREAD + 1) - SPREAD;
            if (dx < 0 || dz < 0 || dx >= WINDOW || dz >= WINDOW) continue;
            int n = surfaces(ground, originX + dx, originZ + dz, ys, biomes);
            int y = nearest(n, ys, biomes, centreY);
            if (y < 0 || taken.get(spot(dx, dz, y))) continue;
            taken.set(spot(dx, dz, y));
            out.plant(this, originX + dx, y + 1, originZ + dz);
        }
    }

    /** A random surface of this plant's biome, or -1. */
    private int pick(Random random, int n, int[] ys, EndBiome[] biomes) {
        int found = 0, y = -1;
        for (int s = 0; s < n; s++) {
            if (biomes[s] == biome && random.nextInt(++found) == 0) y = ys[s];
        }
        return y;
    }

    /** The surface of this plant's biome nearest the patch centre in height, within SPREAD of it, or -1. */
    private int nearest(int n, int[] ys, EndBiome[] biomes, int centreY) {
        int best = -1;
        for (int s = 0; s < n; s++) {
            int off = Math.abs(ys[s] - centreY);
            if (biomes[s] == biome && off <= SPREAD && (best < 0 || off < Math.abs(best - centreY))) best = ys[s];
        }
        return best;
    }

    private static int surfaces(Ground ground, int x, int z, int[] ys, EndBiome[] biomes) {
        int n = Math.min(MAX_SURFACES, ground.surfaces(x, z, ys, biomes));
        // A surface at the build limit has no room above for a plant.
        int kept = 0;
        for (int s = 0; s < n; s++) {
            if (ys[s] < 0 || ys[s] >= 255) continue;
            ys[kept] = ys[s];
            biomes[kept++] = biomes[s];
        }
        return kept;
    }

    private static int spot(int dx, int dz, int y) {
        return (dx * WINDOW + dz) * 256 + y;
    }
}

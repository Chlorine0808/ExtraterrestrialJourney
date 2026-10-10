package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.DensityField;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.builtin.FeatureModifiers;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Steep slopes are not smooth walls. The density grid is 8 blocks wide, so inside a cell a cliff is a smooth blend of
 * its corners; rough faces expose more blocks to the air, while gentle ground stays as it was.
 */
class CliffFacesTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    /**
     * Exposed solid blocks per column, for steep cells (corner spread of 16 or more) and flat ones (under 4), as
     * {steep, flat} with faces() and {steep, flat} from the density grid alone, over the same cells.
     */
    static double[] exposure() {
        double steep = 0, flat = 0, smoothSteep = 0, smoothFlat = 0;
        int steepColumns = 0, flatColumns = 0;
        // Until both kinds of cell are well sampled, however the styles share the land out.
        for (int n = 0; n < 200000 && (steepColumns < 1000 || flatColumns < 1000); n++) {
            int cx = 80 + n % 200 * 5, cz = -1500 + n / 200 * 11;
            ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            if (!plan.shapes()
                .isEmpty()) continue;
            boolean usable = true;
            for (int i = 0; i < 3 && usable; i++) {
                for (int j = 0; j < 3; j++) {
                    ColumnState c = plan.column(cx * 16 + i * 8, cz * 16 + j * 8);
                    if (c.land <= 0 || !c.layers.isEmpty() || c.top > 118) usable = false;
                }
            }
            if (!usable) continue;
            double[] field = new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
            DensityBuilder.fill(plan, field, cx * 2, cz * 2, 0);
            MemorySink smooth = new MemorySink(cx, cz, 0, 128), sink = new MemorySink(cx, cz, 0, 128);
            TallPass.forEachSolid(field, (x, y, z) -> {
                smooth.set(cx * 16 + x, y - TallPass.BASE_Y, cz * 16 + z, EndBlock.STONE);
                sink.set(cx * 16 + x, y - TallPass.BASE_Y, cz * 16 + z, EndBlock.STONE);
            });
            FeatureModifiers.faces()
                .blocks(plan.area(), sink, 1);
            // Cells are told apart on the smooth surface, so both counts cover the same cells.
            int[][] top = new int[16][16];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = 127;
                    while (y > 0 && smooth.get(cx * 16 + x, y, cz * 16 + z) == null) y--;
                    top[x][z] = y;
                }
            }
            double t00 = top[0][0], t10 = top[8][0], t01 = top[0][8], t11 = top[8][8];
            double spread = Math.max(Math.max(t00, t10), Math.max(t01, t11))
                - Math.min(Math.min(t00, t10), Math.min(t01, t11));
            if (spread >= 4 && spread < 16) continue;
            int exposed = exposed(sink, cx, cz), smoothExposed = exposed(smooth, cx, cz);
            if (spread >= 16) {
                steep += exposed;
                smoothSteep += smoothExposed;
                steepColumns += 49;
            } else {
                flat += exposed;
                smoothFlat += smoothExposed;
                flatColumns += 49;
            }
        }
        assertTrue(steepColumns > 400 && flatColumns > 400, steepColumns + " steep, " + flatColumns + " flat columns");
        return new double[] { steep / steepColumns, flat / flatColumns, smoothSteep / steepColumns,
            smoothFlat / flatColumns };
    }

    /** Exposed solid blocks in the 7 x 7 columns of the chunk's first cell, away from the chunk edges. */
    private static int exposed(MemorySink sink, int cx, int cz) {
        int exposed = 0;
        for (int x = 1; x < 8; x++) {
            for (int z = 1; z < 8; z++) {
                for (int y = 1; y < 127; y++) {
                    if (sink.get(cx * 16 + x, y, cz * 16 + z) == null) continue;
                    if (air(sink, cx * 16 + x + 1, y, cz * 16 + z) || air(sink, cx * 16 + x - 1, y, cz * 16 + z)
                        || air(sink, cx * 16 + x, y, cz * 16 + z + 1)
                        || air(sink, cx * 16 + x, y, cz * 16 + z - 1)
                        || air(sink, cx * 16 + x, y + 1, cz * 16 + z)) exposed++;
                }
            }
        }
        return exposed;
    }

    @Test
    void steepFacesAreRoughAndFlatGroundIsNot() {
        double[] s = exposure();
        // Against the same cells as the density grid alone leaves them: rough steep faces expose clearly more blocks,
        // while flat ground stays nearly as it was.
        assertTrue(s[0] > s[2] + 0.1, "steep faces are smooth: " + s[0] + " against " + s[2]);
        assertTrue(s[1] < s[3] + 0.03, "flat ground got rough: " + s[1] + " against " + s[3]);
    }

    private static boolean air(MemorySink sink, int x, int y, int z) {
        return sink.get(x, y, z) == null;
    }

    @Test
    void facesMoveTheSurfaceOnlyAFewBlocks() {
        // Every block faces() changes must lie within a few blocks of the surface it started from: no spikes.
        int chunks = 0, changed = 0;
        for (int n = 0; n < 20000 && chunks < 60; n++) {
            int cx = 80 + n % 200 * 5, cz = -1500 + n / 200 * 11;
            ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
            if (plan.column(cx * 16 + 8, cz * 16 + 8).land <= 0) continue;
            chunks++;
            for (boolean upper : new boolean[] { false, true }) {
                int base = upper ? 128 : 0;
                double[] f = plan.densityField(upper, true);
                boolean[][][] was = new boolean[16][128][16];
                MemorySink sink = new MemorySink(cx, cz, base, base + 128);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 128; y++) {
                            was[x][y][z] = DensityField.at(f, x, y, z) > 0;
                            if (was[x][y][z]) sink.set(cx * 16 + x, base + y, cz * 16 + z, EndBlock.STONE);
                        }
                    }
                }
                FeatureModifiers.faces()
                    .blocks(plan.area(), sink, 1);
                for (int x = 5; x < 11; x++) {
                    for (int z = 5; z < 11; z++) {
                        for (int y = 5; y < 123; y++) {
                            boolean now = sink.get(cx * 16 + x, base + y, cz * 16 + z) != null;
                            if (now == was[x][y][z]) continue;
                            changed++;
                            // A face moves at most FACE_DEPTH (4) blocks, plus a block for rounding.
                            assertTrue(
                                nearSurface(was, x, y, z, 6),
                                "changed far from the surface at " + x + "," + (base + y) + "," + z);
                        }
                    }
                }
            }
        }
        assertTrue(changed > 0, "faces changed nothing");
    }

    /** Whether a block of the other kind lies within r blocks. */
    private static boolean nearSurface(boolean[][][] solid, int x, int y, int z, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    int px = x + dx, py = y + dy, pz = z + dz;
                    if (px < 0 || px > 15 || py < 0 || py > 127 || pz < 0 || pz > 15) continue;
                    if (solid[px][py][pz] != solid[x][y][z]) return true;
                }
            }
        }
        return false;
    }
}

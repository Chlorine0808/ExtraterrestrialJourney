package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.plant.GiantTree;
import chlorine.etjourney.world.end.feature.plant.GiantTrees;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * A giant tree drawn by the block pass in real chunks: both halves of a column and neighbouring chunks agree, the
 * trunk reaches the real ground, and the tree stands on grove ground.
 */
class GiantTreeDrawTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final double[] ANCHOR = firstAnchor();
    private static final int BASE = (int) Math.floor(ANCHOR[1]);
    private static final int CX = (int) Math.floor(ANCHOR[0]) >> 4, CZ = (int) Math.floor(ANCHOR[2]) >> 4;
    private static final Map<Long, Chunk> DRAWN = new HashMap<>();

    /**
     * The first giant tree found scanning outwards from 1500 blocks east of the centre whose foot reaches below Y 128,
     * so both halves of a column hold part of it.
     */
    private static double[] firstAnchor() {
        for (int cx = 94; cx < 400; cx++) {
            for (int cz = -40; cz <= 40; cz++) {
                Continent.Seed s = Continent.seedAt(SEED, cx, cz);
                if (s == null) continue;
                List<double[]> anchors = GiantTrees.anchors(SEED, s, SAMPLER.structureProbe());
                if (!anchors.isEmpty() && anchors.get(0)[1] < 140) return anchors.get(0);
            }
        }
        return null;
    }

    /** The chunk as the game builds it, grid rock first and then the block pass, into a sink of the given range. */
    private static MemorySink generate(int cx, int cz, int minY, int maxY) {
        ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
        MemorySink sink = new MemorySink(cx, cz, minY, maxY);
        if (minY < 128) TallPass.forEachSolid(
            plan.densityField(false, true),
            (x, y, z) -> sink.set(cx * 16 + x, y - TallPass.BASE_Y, cz * 16 + z, EndBlock.STONE));
        if (maxY > 128 && TallPass.needed(plan)) TallPass.forEachSolid(
            TallPass.upperField(plan),
            (x, y, z) -> { if (y <= 255) sink.set(cx * 16 + x, y, cz * 16 + z, EndBlock.STONE); });
        plan.blocks(sink);
        return sink;
    }

    /** Both halves of the chunk as the game builds them, joined; built once for every test. */
    private static synchronized Chunk drawn(int cx, int cz) {
        return DRAWN.computeIfAbsent(
            ((long) cx << 32) ^ (cz & 0xFFFFFFFFL),
            k -> new Chunk(generate(cx, cz, 0, 128), generate(cx, cz, 128, 256)));
    }

    /** Reads the blocks of a chunk. */
    private interface Blocks {

        EndBlock get(int x, int y, int z);
    }

    private static final class Chunk implements Blocks {

        final MemorySink low, high;

        Chunk(MemorySink low, MemorySink high) {
            this.low = low;
            this.high = high;
        }

        @Override
        public EndBlock get(int x, int y, int z) {
            return y < 128 ? low.get(x, y, z) : high.get(x, y, z);
        }
    }

    /** The tree alone drawn into an empty chunk. */
    private static Blocks alone(int cx, int cz) {
        MemorySink sink = new MemorySink(cx, cz, 0, 256);
        new GiantTree(SEED, ANCHOR, SAMPLER.structureProbe()).draw(sink);
        return sink::get;
    }

    private static boolean log(Blocks chunk, int x, int y, int z) {
        return chunk.get(x, y, z) == EndBlock.GROVE_LOG;
    }

    private static boolean tree(EndBlock b) {
        return b == EndBlock.GROVE_LOG || b == EndBlock.GROVE_LEAVES || b == EndBlock.GROVE_BLOSSOM;
    }

    /** Height of the lowest log in the column, or -1. */
    private static int lowestLog(Blocks chunk, int x, int z) {
        for (int y = 0; y < 256; y++) if (log(chunk, x, y, z)) return y;
        return -1;
    }

    private static boolean hasLog(Blocks chunk, int cx, int cz) {
        for (int x = cx * 16; x < cx * 16 + 16; x++) {
            for (int z = cz * 16; z < cz * 16 + 16; z++) if (lowestLog(chunk, x, z) >= 0) return true;
        }
        return false;
    }

    @Test
    void halvesAgree() {
        assertNotNull(ANCHOR, "no giant tree found");
        MemorySink whole = generate(CX, CZ, 0, 256);
        Chunk halves = drawn(CX, CZ);
        int logs = 0, lowLogs = 0;
        for (int x = CX * 16; x < CX * 16 + 16; x++) {
            for (int z = CZ * 16; z < CZ * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    assertEquals(whole.get(x, y, z), halves.get(x, y, z), "halves differ at " + x + "," + y + "," + z);
                    if (!log(halves, x, y, z)) continue;
                    logs++;
                    if (y < 128) lowLogs++;
                }
            }
        }
        System.out.println(
            "giant tree: root at " + ANCHOR[0]
                + ","
                + ANCHOR[1]
                + ","
                + ANCHOR[2]
                + ", "
                + logs
                + " logs in its chunk, "
                + lowLogs
                + " below Y 128");
        assertTrue(lowLogs > 0 && logs > lowLogs, "the tree does not cross Y 128: " + lowLogs + " of " + logs + " low");
    }

    @Test
    void neighboursAgree() {
        // The root sits mid-chunk, so the trunk crosses the border with the chunk to the east.
        Chunk west = drawn(CX, CZ), east = drawn(CX + 1, CZ);
        Blocks westShape = alone(CX, CZ), eastShape = alone(CX + 1, CZ);
        int border = (CX + 1) * 16, across = 0;
        for (int z = CZ * 16; z < CZ * 16 + 16; z++) {
            for (int y = BASE + 2; y < BASE + 45; y++) {
                if (!log(westShape, border - 1, y, z) || !log(eastShape, border, y, z)) continue;
                // Ground on one side may hold the wood back; air there would leave a cut face.
                boolean w = log(west, border - 1, y, z), e = log(east, border, y, z);
                assertTrue(
                    !w || e || east.get(border, y, z) != null,
                    "the trunk breaks off east of y " + y + ", z " + z);
                assertTrue(
                    !e || w || west.get(border - 1, y, z) != null,
                    "the trunk breaks off west of y " + y + ", z " + z);
                if (w && e) across++;
            }
        }
        System.out.println("giant tree: " + across + " trunk blocks continue across the chunk border");
        assertTrue(across > 50, "only " + across + " trunk blocks across the border");
    }

    @Test
    void trunkMeetsTheRealGround() {
        int columns = 0, lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        for (int cx = CX - 1; cx <= CX + 1; cx++) {
            for (int cz = CZ - 1; cz <= CZ + 1; cz++) {
                Chunk chunk = drawn(cx, cz);
                for (int x = cx * 16; x < cx * 16 + 16; x++) {
                    for (int z = cz * 16; z < cz * 16 + 16; z++) {
                        if (Math.hypot(x + 0.5 - ANCHOR[0], z + 0.5 - ANCHOR[2]) > 12) continue;
                        int y = lowestLog(chunk, x, z);
                        // Columns under a branch alone, beyond the narrow parts of the foot.
                        if (y < 0 || y >= BASE + 10) continue;
                        columns++;
                        lowest = Math.min(lowest, y);
                        highest = Math.max(highest, y);
                        assertTrue(y > 0 && chunk.get(x, y - 1, z) != null, "trunk floats at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        System.out.println(
            "giant tree: " + columns
                + " trunk columns, lowest logs "
                + (lowest - BASE)
                + " to "
                + (highest - BASE)
                + " from the root");
        assertTrue(columns > 100, "only " + columns + " trunk columns");
    }

    /**
     * The ground next to each column of trunk or root wood, near its lowest log, is grove ground. The wood overwrites
     * the surface under the tree, so the surface is read from the nearest column without trunk or root wood; columns
     * with none within 4 (the closed foot of the trunk) are skipped.
     */
    @Test
    void onlyOverGroveTop() {
        int r = 2, x0 = (CX - r) * 16, z0 = (CZ - r) * 16, side = (2 * r + 1) * 16;
        Blocks area = (x, y, z) -> drawn(x >> 4, z >> 4).get(x, y, z);
        int[][] lowest = new int[side][side];
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int y = lowestLog(area, x0 + i, z0 + j);
                // Columns under a branch alone stand well above the ground and count as open.
                lowest[i][j] = y >= BASE + 10 ? -1 : y;
            }
        }
        int columns = 0, grove = 0, inside = 0;
        Map<String, Integer> others = new HashMap<>();
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                if (lowest[i][j] < 0) continue;
                int[] open = nearestOpen(lowest, i, j);
                if (open == null) {
                    inside++;
                    continue;
                }
                columns++;
                EndBlock ground = null;
                for (int y = Math.min(255, lowest[i][j] + 16); y >= Math.max(0, lowest[i][j] - 6)
                    && ground == null; y--) {
                    EndBlock block = area.get(x0 + open[0], y, z0 + open[1]);
                    if (block != null && !tree(block)) ground = block;
                }
                if (ground == EndBlock.GROVE_TOP || ground == EndBlock.GROVE_FILL || ground == EndBlock.GROVE_LOG)
                    grove++;
                else others.merge(String.valueOf(ground), 1, Integer::sum);
            }
        }
        double share = grove / (double) columns;
        System.out.println(
            "giant tree: " + grove
                + " of "
                + columns
                + " wood columns over grove ground ("
                + inside
                + " inside the foot), others "
                + others);
        assertTrue(columns > 300, "only " + columns + " wood columns");
        assertTrue(share >= 0.9, "only " + share + " over grove ground; others " + others);
    }

    /** The nearest column within 4 of (i, j) without trunk or root wood, or null. */
    private static int[] nearestOpen(int[][] lowest, int i, int j) {
        int side = lowest.length;
        for (int d = 1; d <= 4; d++) {
            for (int di = -d; di <= d; di++) {
                for (int dj = -d; dj <= d; dj++) {
                    if (Math.max(Math.abs(di), Math.abs(dj)) != d) continue;
                    int a = i + di, b = j + dj;
                    if (a >= 0 && b >= 0 && a < side && b < side && lowest[a][b] < 0) return new int[] { a, b };
                }
            }
        }
        return null;
    }

    @Test
    void notOnReservedGround() {
        // Reserved only as seen from the root's chunk, so the drawn chunk's own terrain is unchanged.
        List<Area> reserved = Collections.singletonList(new Area("test", ANCHOR[0], ANCHOR[2], 8));
        MemorySink sink = new MemorySink(CX + 1, CZ, 0, 256);
        new ChunkPlan(SAMPLER, CX + 1, CZ, (a, b) -> a == CX && b == CZ ? reserved : Collections.emptyList())
            .blocks(sink);
        assertTrue(!hasLog(sink::get, CX + 1, CZ), "tree drawn on reserved ground");
        assertTrue(hasLog(drawn(CX + 1, CZ), CX + 1, CZ), "the tree does not reach the chunk without the reservation");
    }

    /**
     * An area beside the tree, not touching its footprint, whose fade ring lowers the ground under the roots on that
     * side: the probe does not see the fade, so the roots would float, and the tree is left out.
     */
    @Test
    void notInAReservedFade() {
        double radius = 8, x = ANCHOR[0] + GiantTrees.FOOTPRINT + 1.2 * radius + 2;
        List<Area> reserved = Collections.singletonList(new Area("test", x, ANCHOR[2], radius));
        assertTrue(!Reservations.touches(reserved, ANCHOR[0], ANCHOR[2], GiantTrees.FOOTPRINT));
        MemorySink sink = new MemorySink(CX - 1, CZ, 0, 256);
        new ChunkPlan(SAMPLER, CX - 1, CZ, (a, b) -> reserved).blocks(sink);
        assertTrue(!hasLog(sink::get, CX - 1, CZ), "tree drawn beside a reserved area");
        assertTrue(hasLog(drawn(CX - 1, CZ), CX - 1, CZ), "the tree does not reach the chunk without the reservation");
    }
}

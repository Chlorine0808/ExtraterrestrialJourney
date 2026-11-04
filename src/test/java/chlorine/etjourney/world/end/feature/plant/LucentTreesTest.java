package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Lucentwoods trees placed per cell from the seed alone and drawn chunk by chunk into air. */
class LucentTreesTest {

    private static LucentTrees.Terrain flat(final double ground) {
        return new LucentTrees.Terrain() {

            @Override
            public double ground(double x, double z) {
                return ground;
            }

            @Override
            public boolean lucent(double x, double z) {
                return x >= 0;
            }
        };
    }

    @Test
    void sameSeedSamePlaces() {
        LucentTrees.Terrain t = flat(60);
        List<LucentTrees.Anchor> a = LucentTrees.in(7L, 3, 4, t), b = LucentTrees.in(7L, 3, 4, t);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x);
            assertEquals(a.get(i).y, b.get(i).y);
            assertEquals(a.get(i).z, b.get(i).z);
            assertEquals(a.get(i).shape.size(), b.get(i).shape.size());
        }
    }

    @Test
    void zeroToThreePerCell() {
        LucentTrees.Terrain t = flat(60);
        int total = 0;
        for (int i = 0; i < 200; i++) {
            int n = LucentTrees.in(11L, 1 + i % 20, i / 20, t)
                .size();
            assertTrue(n >= 0 && n <= 3);
            total += n;
        }
        assertEquals(1.5, total / 200.0, 0.25);
    }

    @Test
    void onlyOnLucentwoods() {
        LucentTrees.Terrain t = flat(60);
        int seen = 0;
        for (int cx = -5; cx < 5; cx++)
            for (int cz = 0; cz < 3; cz++) for (LucentTrees.Anchor a : LucentTrees.in(5L, cx, cz, t)) {
                assertTrue(a.x >= 0);
                seen++;
            }
        assertTrue(seen > 0);
    }

    @Test
    void highGroundFits() {
        LucentTrees.Terrain t = flat(235);
        for (int i = 0; i < 20; i++)
            for (LucentTrees.Anchor a : LucentTrees.in(9L, 1 + i, 0, t)) assertTrue(a.y + a.shape.maxY() <= 250);
    }

    /** What the nine chunks around (cx, cz) would draw, merged into one ordered list. */
    private static List<LucentTrees.Anchor> around(long seed, int cx, int cz, LucentTrees.Terrain t) {
        Set<String> seen = new HashSet<>();
        List<LucentTrees.Anchor> all = new ArrayList<>();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) for (LucentTrees.Anchor a : LucentTrees.reaching(seed, cx + i, cz + j, t))
                if (seen.add(a.x + "," + a.z + "," + a.order)) all.add(a);
        Collections.sort(all, (a, b) -> {
            int c = Long.compare(a.order, b.order);
            if (c != 0) return c;
            c = Integer.compare(a.x, b.x);
            return c != 0 ? c : Integer.compare(a.z, b.z);
        });
        return all;
    }

    private static Map<Long, EndBlock> perChunk(long seed, int cx, int cz, LucentTrees.Terrain t, int y0, int y1) {
        Map<Long, EndBlock> cells = new HashMap<>();
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            MapSink sink = new MapSink(cells, (cx + i) * 16, (cz + j) * 16, 16, y0, y1);
            LucentTrees.draw(LucentTrees.reaching(seed, cx + i, cz + j, t), sink);
        }
        return cells;
    }

    private static Map<Long, EndBlock> onePiece(long seed, int cx, int cz, LucentTrees.Terrain t, int y0, int y1) {
        Map<Long, EndBlock> cells = new HashMap<>();
        LucentTrees.draw(around(seed, cx, cz, t), new MapSink(cells, (cx - 1) * 16, (cz - 1) * 16, 48, y0, y1));
        return cells;
    }

    /** Overlapping crowns: each chunk draws its own part, and together they match one draw of the whole. */
    @Test
    void chunksJoinLikeOneDraw() {
        LucentTrees.Terrain t = flat(60);
        Map<Long, EndBlock> joined = perChunk(21L, 11, 11, t, 0, 256);
        assertTrue(joined.size() > 1000);
        assertEquals(onePiece(21L, 11, 11, t, 0, 256), joined);
    }

    @Test
    void splitPassesMatchOnePass() {
        // Ground at 120 puts the crowns and their vines across the split at 128.
        LucentTrees.Terrain t = flat(120);
        Map<Long, EndBlock> whole = perChunk(4L, 9, 9, t, 0, 256);
        Map<Long, EndBlock> split = perChunk(4L, 9, 9, t, 0, 128);
        split.putAll(perChunk(4L, 9, 9, t, 128, 256));
        assertTrue(whole.size() > 1000);
        assertEquals(whole, split);
    }

    /** A run blocked anywhere by terrain is left out whole, in every chunk alike. */
    @Test
    void hangingRunIsWholeOrAbsent() {
        LucentTrees.Terrain t = flat(60);
        Map<Long, EndBlock> cells = new HashMap<>();
        // Rock from y 66 to 67 in every column: it cuts across some runs and blocks them.
        for (int x = 9 * 16; x < 12 * 16; x++) for (int z = 9 * 16; z < 12 * 16; z++)
            for (int y = 66; y <= 67; y++) cells.put(MapSink.key(x, y, z), EndBlock.STONE);
        Map<Long, EndBlock> blocked = new HashMap<>(cells);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) LucentTrees.draw(
            LucentTrees.reaching(31L, 10 + i, 10 + j, t),
            new MapSink(blocked, (10 + i) * 16, (10 + j) * 16, 16, 0, 256));
        int runs = 0;
        for (int x = 9 * 16; x < 12 * 16; x++) for (int z = 9 * 16; z < 12 * 16; z++) {
            for (int y = 61; y < 200; y++) {
                EndBlock b = blocked.get(MapSink.key(x, y, z));
                boolean top = (b == EndBlock.LUCENT_VINE || b == EndBlock.LUCENT_VINE_LAST
                    || b == EndBlock.LUCENT_LANTERN) && blocked.get(MapSink.key(x, y + 1, z)) == EndBlock.LUCENT_LEAVES;
                if (!top) continue;
                runs++;
                // Walk down the run: it must end in its tip or lantern, never be cut short by the rock.
                int k = y;
                while (blocked.get(MapSink.key(x, k, z)) == EndBlock.LUCENT_VINE) k--;
                EndBlock end = blocked.get(MapSink.key(x, k, z));
                assertTrue(
                    end == EndBlock.LUCENT_VINE_LAST || end == EndBlock.LUCENT_LANTERN,
                    "cut at " + x + "," + k + "," + z);
            }
        }
        assertTrue(runs > 20, runs + " runs");
    }
}

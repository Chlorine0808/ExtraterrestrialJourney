package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** BetterEnd's lucernia: thin branches fanning out of one foot, a flat-bottomed leaf ball on each, vines below. */
class LucentTreeTest {

    private static final LucentTree.Ground FLAT = (dx, dz) -> -1;

    /** Spreads small test indices over the seed space; Random's first draws barely move for 1, 2, 3, ... */
    private static long seedOf(long i) {
        return i * 0x9E3779B97F4A7C15L;
    }

    private static LucentTree tree(long i) {
        LucentTree t = LucentTree.of(seedOf(i), 12 + i % 9, 200, FLAT);
        assertNotNull(t, "seed " + i);
        return t;
    }

    private static boolean hanging(EndBlock b) {
        return b == EndBlock.LUCENT_VINE || b == EndBlock.LUCENT_VINE_LAST || b == EndBlock.LUCENT_LANTERN;
    }

    private static Map<Long, EndBlock> cells(LucentTree t) {
        Map<Long, EndBlock> m = new HashMap<>();
        t.forEach(
            (x, y, z, b) -> assertEquals(null, m.put(SdfFill.key(x, y, z), b), "twice at " + x + "," + y + "," + z));
        return m;
    }

    /** Runs as forEach hands them out, top to bottom; each ends with a tip or a lantern. */
    private static List<List<int[]>> runs(LucentTree t) {
        List<List<int[]>> runs = new ArrayList<>();
        List<int[]> open = new ArrayList<>();
        t.forEach((x, y, z, b) -> {
            if (!hanging(b)) return;
            open.add(new int[] { x, y, z, b.ordinal() });
            if (b != EndBlock.LUCENT_VINE) {
                runs.add(new ArrayList<>(open));
                open.clear();
            }
        });
        assertTrue(open.isEmpty(), "a run without its end");
        return runs;
    }

    @Test
    void branchCountFollowsSize() {
        for (long i = 1; i <= 30; i++) {
            LucentTree t = tree(i);
            assertEquals((int) (t.size() * 0.3), t.branches(), "seed " + i);
            assertTrue(t.branches() >= 3 && t.branches() <= 6, "seed " + i);
        }
    }

    @Test
    void oneConnectedBody() {
        for (long i = 1; i <= 15; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            Voxels v = new Voxels();
            for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
                if (hanging(e.getValue())) continue;
                v.put(SdfFill.x(e.getKey()), SdfFill.y(e.getKey()) + 100, SdfFill.z(e.getKey()), e.getValue());
            }
            assertEquals(v.cells.size(), v.connected(0, 100, 0), "seed " + i);
        }
    }

    @Test
    void furOnlyBesideLeavesAndNotBelow() {
        int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (long i = 1; i <= 15; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            int fur = 0;
            for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
                if (e.getValue() != EndBlock.LUCENT_FUR) continue;
                fur++;
                int x = SdfFill.x(e.getKey()), y = SdfFill.y(e.getKey()), z = SdfFill.z(e.getKey());
                assertFalse(m.get(SdfFill.key(x, y + 1, z)) == EndBlock.LUCENT_LEAVES, "seed " + i);
                boolean beside = false;
                for (int[] f : faces)
                    beside |= m.get(SdfFill.key(x + f[0], y + f[1], z + f[2])) == EndBlock.LUCENT_LEAVES;
                assertTrue(beside, "seed " + i + " fur at " + x + "," + y + "," + z);
            }
            assertTrue(fur > 0, "seed " + i + " has no fur");
        }
    }

    @Test
    void hangingRunsSitUnderLeaves() {
        for (long i = 1; i <= 20; i++) {
            LucentTree t = tree(i);
            Map<Long, EndBlock> m = cells(t);
            for (List<int[]> run : runs(t)) {
                int[] top = run.get(0);
                assertEquals(EndBlock.LUCENT_LEAVES, m.get(SdfFill.key(top[0], top[1] + 1, top[2])), "seed " + i);
                for (int k = 1; k < run.size(); k++) {
                    assertEquals(top[0], run.get(k)[0]);
                    assertEquals(top[1] - k, run.get(k)[1]);
                    assertEquals(top[2], run.get(k)[2]);
                }
                EndBlock last = EndBlock.values()[run.get(run.size() - 1)[3]];
                if (last == EndBlock.LUCENT_LANTERN) assertTrue(run.size() >= 2 && run.size() <= 4, "seed " + i);
                else assertTrue(run.size() >= 1 && run.size() <= 8, "seed " + i);
            }
        }
    }

    @Test
    void someRunsCarryLanterns() {
        int runs = 0, lanterns = 0;
        for (long i = 1; i <= 20; i++) {
            for (List<int[]> run : runs(tree(i))) {
                runs++;
                if (run.get(run.size() - 1)[3] == EndBlock.LUCENT_LANTERN.ordinal()) lanterns++;
            }
        }
        assertTrue(runs > 100, runs + " runs");
        assertTrue(lanterns > runs * 0.15 && lanterns < runs * 0.45, lanterns + " of " + runs);
    }

    @Test
    void fitsUnderTheCap() {
        for (long i = 1; i <= 20; i++) {
            LucentTree t = LucentTree.of(seedOf(i), 20, 18, FLAT);
            if (t != null) assertTrue(t.maxY() <= 18, "seed " + i);
        }
    }

    @Test
    void tooLowIsRefused() {
        assertNull(LucentTree.of(seedOf(3), 20, 4, FLAT));
    }

    @Test
    void leavesReachWood() {
        for (long i = 1; i <= 20; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            assertTrue(
                LeafReach.worst(m, k -> new int[] { SdfFill.x(k), SdfFill.y(k), SdfFill.z(k) }) <= Grower.LEAF_REACH,
                "seed " + i);
        }
    }

    @Test
    void rootsOnlyReachingTheGround() {
        // Upstream roots start 3 to 5 up and only the larger trees' tips reach the ground; none reach it 40 below.
        int grounded = 0, floating = 0;
        for (long i = 1; i <= 10; i++) {
            grounded += cells(LucentTree.of(seedOf(i), 20, 200, FLAT)).size();
            floating += cells(LucentTree.of(seedOf(i), 20, 200, (dx, dz) -> -40)).size();
        }
        assertTrue(grounded > floating, grounded + " vs " + floating);
    }

    @Test
    void sameSeedSameTree() {
        assertEquals(cells(tree(7)), cells(tree(7)));
    }
}

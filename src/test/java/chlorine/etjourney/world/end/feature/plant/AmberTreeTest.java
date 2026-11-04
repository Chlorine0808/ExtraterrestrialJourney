package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The amber seed tree: twisted strands hugging a glowing drop and joining into one trunk above it. */
class AmberTreeTest {

    private static final AmberTree.Ground FLAT = (dx, dz) -> -1;
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static long seedOf(long i) {
        return i * 0x9E3779B97F4A7C15L;
    }

    private static AmberTree tree(long i) {
        AmberTree t = AmberTree.of(seedOf(i), 250, FLAT);
        assertNotNull(t, "seed " + i);
        return t;
    }

    private static Map<Long, EndBlock> cells(AmberTree t) {
        Map<Long, EndBlock> m = new HashMap<>();
        t.forEach((dx, dy, dz, b) -> m.put(SdfFill.key(dx, dy, dz), b));
        return m;
    }

    /** Face-connected pieces of the given block, largest first. */
    private static List<Set<Long>> pieces(Map<Long, EndBlock> m, EndBlock kind) {
        List<Set<Long>> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
            if (e.getValue() != kind || seen.contains(e.getKey())) continue;
            Set<Long> piece = new HashSet<>();
            ArrayDeque<Long> open = new ArrayDeque<>();
            open.add(e.getKey());
            seen.add(e.getKey());
            while (!open.isEmpty()) {
                long c = open.poll();
                piece.add(c);
                for (int[] f : FACES) {
                    long n = SdfFill.offset(c, f[0], f[1], f[2]);
                    if (m.get(n) == kind && seen.add(n)) open.add(n);
                }
            }
            out.add(piece);
        }
        out.sort((a, b) -> b.size() - a.size());
        return out;
    }

    @Test
    void sameSeedSameTree() {
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        AmberTree.of(42, 250, FLAT)
            .forEach((x, y, z, k) -> a.add(x + "," + y + "," + z + k));
        AmberTree.of(42, 250, FLAT)
            .forEach((x, y, z, k) -> b.add(x + "," + y + "," + z + k));
        assertEquals(a, b);
    }

    @Test
    void staysWithinItsFootprint() {
        for (long i = 0; i < 100; i++) {
            final long s = i;
            tree(i).forEach((dx, dy, dz, k) -> {
                assertTrue(Math.hypot(dx, dz) <= 11, "seed " + s);
                assertTrue(dy <= 18, "seed " + s);
            });
        }
    }

    @Test
    void standsAboutFifteenTall() {
        for (long i = 0; i < 100; i++) {
            int top = tree(i).maxY();
            assertTrue(top >= 12 && top <= 18, "seed " + i + " top " + top);
        }
    }

    @Test
    void seedIsAWideDrop() {
        for (long i = 0; i < 100; i++) {
            Set<Long> body = pieces(cells(tree(i)), EndBlock.AMBER_SEED).get(0);
            double mx = 0, mz = 0;
            int y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
            for (long c : body) {
                mx += SdfFill.x(c);
                mz += SdfFill.z(c);
                y0 = Math.min(y0, SdfFill.y(c));
                y1 = Math.max(y1, SdfFill.y(c));
            }
            mx /= body.size();
            mz /= body.size();
            double r = 0;
            for (long c : body) r = Math.max(r, Math.hypot(SdfFill.x(c) - mx, SdfFill.z(c) - mz));
            assertTrue(r >= 3 && r <= 5.5, "seed " + i + " radius " + r);
            assertTrue(y1 - y0 + 1 >= 4 && y1 - y0 + 1 <= 8, "seed " + i + " height " + (y1 - y0 + 1));
            assertTrue(y0 <= 1, "seed " + i + " bottom " + y0);
        }
    }

    @Test
    void seedShowsThroughTheGaps() {
        double sum = 0;
        for (long i = 0; i < 100; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            Set<Long> body = pieces(m, EndBlock.AMBER_SEED).get(0);
            double mx = 0, mz = 0;
            for (long c : body) {
                mx += SdfFill.x(c);
                mz += SdfFill.z(c);
            }
            mx /= body.size();
            mz /= body.size();
            int shell = 0, open = 0;
            for (long c : body) {
                int x = SdfFill.x(c), y = SdfFill.y(c), z = SdfFill.z(c);
                double ox = x - mx, oz = z - mz;
                if (ox == 0 && oz == 0) continue;
                int sx = Math.abs(ox) >= Math.abs(oz) ? (int) Math.signum(ox) : 0;
                int sz = sx == 0 ? (int) Math.signum(oz) : 0;
                long out = SdfFill.key(x + sx, y, z + sz);
                if (body.contains(out)) continue;
                shell++;
                if (m.get(out) != EndBlock.AMBER_ROOT) open++;
            }
            assertTrue(open >= shell * 0.45, "seed " + i + " open " + open + " of " + shell);
            sum += (double) open / shell;
        }
        assertTrue(sum / 100 >= 0.6, "mean " + sum / 100);
    }

    @Test
    void strandsVary() {
        Set<Integer> counts = new HashSet<>();
        for (long i = 0; i < 50; i++) {
            int n = tree(i).strands();
            assertTrue(n >= 4 && n <= 6, "seed " + i);
            counts.add(n);
        }
        assertTrue(counts.size() >= 3);
    }

    @Test
    void woodIsOnePiece() {
        for (long i = 0; i < 20; i++) assertEquals(1, pieces(cells(tree(i)), EndBlock.AMBER_ROOT).size(), "seed " + i);
    }

    @Test
    void fruitHangsOnBranches() {
        int fruits = 0;
        for (long i = 0; i < 20; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            List<Set<Long>> seeds = pieces(m, EndBlock.AMBER_SEED);
            assertTrue(seeds.size() >= 2, "seed " + i);
            for (Set<Long> fruit : seeds.subList(1, seeds.size())) {
                boolean touches = false;
                for (long c : fruit) for (int[] f : FACES)
                    if (m.get(SdfFill.offset(c, f[0], f[1], f[2])) == EndBlock.AMBER_ROOT) touches = true;
                assertTrue(touches, "seed " + i);
                fruits++;
            }
        }
        assertTrue(fruits >= 40, "fruits " + fruits);
    }

    @Test
    void rootsFollowTheGround() {
        AmberTree.Ground slope = (dx, dz) -> Math.floorDiv(dx, 2) - 1;
        for (long i = 0; i < 20; i++) {
            AmberTree t = AmberTree.of(seedOf(i), 250, slope);
            assertNotNull(t);
            int[] near = { 0, 0 };
            t.forEach((dx, dy, dz, k) -> {
                if (k != EndBlock.AMBER_ROOT || Math.hypot(dx, dz) < 8.5 || dy > 6) return;
                near[1]++;
                if (Math.abs(dy - (slope.top(dx, dz) + 1)) <= 1) near[0]++;
            });
            assertTrue(near[1] > 0, "seed " + i);
            assertTrue(near[0] >= near[1] * 0.8, "seed " + i + " " + near[0] + " of " + near[1]);
        }
    }

    @Test
    void shrinksUnderALowCeiling() {
        for (long i = 0; i < 20; i++) {
            AmberTree t = AmberTree.of(seedOf(i), 12, FLAT);
            assertTrue(t == null || t.maxY() <= 12, "seed " + i);
        }
        assertNull(AmberTree.of(seedOf(1), 5, FLAT));
    }
}

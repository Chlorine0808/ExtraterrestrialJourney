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
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The amber seed tree: twisted strands hugging a glowing drop from the ground and joining into one trunk above it. */
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
    private static List<Set<Long>> pieces(Map<Long, EndBlock> m, Predicate<EndBlock> kind) {
        List<Set<Long>> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
            if (!kind.test(e.getValue()) || seen.contains(e.getKey())) continue;
            Set<Long> piece = new HashSet<>();
            ArrayDeque<Long> open = new ArrayDeque<>();
            open.add(e.getKey());
            seen.add(e.getKey());
            while (!open.isEmpty()) {
                long c = open.poll();
                piece.add(c);
                for (int[] f : FACES) {
                    long n = SdfFill.offset(c, f[0], f[1], f[2]);
                    if (m.containsKey(n) && kind.test(m.get(n)) && seen.add(n)) open.add(n);
                }
            }
            out.add(piece);
        }
        out.sort((a, b) -> b.size() - a.size());
        return out;
    }

    private static boolean isSeed(EndBlock b) {
        return b == EndBlock.AMBER_SEED;
    }

    /** The seed body, the largest piece of seed. */
    private static Set<Long> body(Map<Long, EndBlock> m) {
        return pieces(m, AmberTreeTest::isSeed).get(0);
    }

    /** One horizontal step away from the body's centre (mx, mz), along the larger axis; null at the centre. */
    private static int[] outward(long c, double mx, double mz) {
        double ox = SdfFill.x(c) - mx, oz = SdfFill.z(c) - mz;
        if (ox == 0 && oz == 0) return null;
        int sx = Math.abs(ox) >= Math.abs(oz) ? (int) Math.signum(ox) : 0;
        int sz = sx == 0 ? (int) Math.signum(oz) : 0;
        return new int[] { sx, sz };
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
                assertTrue(Math.hypot(dx, dz) <= 12.5, "seed " + s);
                assertTrue(dy <= 25, "seed " + s);
            });
        }
    }

    @Test
    void standsAboutTwentyTall() {
        for (long i = 0; i < 100; i++) {
            int top = tree(i).maxY();
            assertTrue(top >= 19 && top <= 25, "seed " + i + " top " + top);
        }
    }

    @Test
    void seedIsAWideDrop() {
        for (long i = 0; i < 100; i++) {
            Set<Long> body = body(cells(tree(i)));
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
            assertTrue(r >= 7.5 && r <= 11.5, "seed " + i + " radius " + r);
            assertTrue(y1 - y0 + 1 >= 10 && y1 - y0 + 1 <= 16, "seed " + i + " height " + (y1 - y0 + 1));
            assertTrue(y0 < 0, "seed " + i + " bottom " + y0);
        }
    }

    @Test
    void seedShowsThroughTheGaps() {
        double sum = 0;
        for (long i = 0; i < 100; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            Set<Long> body = body(m);
            double mx = 0, mz = 0;
            for (long c : body) {
                mx += SdfFill.x(c);
                mz += SdfFill.z(c);
            }
            mx /= body.size();
            mz /= body.size();
            int shell = 0, open = 0;
            for (long c : body) {
                int[] step = outward(c, mx, mz);
                if (step == null) continue;
                long out = SdfFill.offset(c, step[0], 0, step[1]);
                if (body.contains(out)) continue;
                shell++;
                if (!AmberTree.wood(m.get(out))) open++;
            }
            assertTrue(open >= shell * 0.45, "seed " + i + " open " + open + " of " + shell);
            sum += (double) open / shell;
        }
        assertTrue(sum / 100 >= 0.5, "mean " + sum / 100);
    }

    @Test
    void noAirBetweenSeedAndStrands() {
        // A strand that hugs the seed touches it; air with wood just beyond it is the gap the user saw.
        // Below y = 0 lies the flat ground, so no air shows there.
        for (long i = 0; i < 100; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            Set<Long> body = body(m);
            double mx = 0, mz = 0;
            for (long c : body) {
                mx += SdfFill.x(c);
                mz += SdfFill.z(c);
            }
            mx /= body.size();
            mz /= body.size();
            int shell = 0, gaps = 0;
            for (long c : body) {
                int[] step = outward(c, mx, mz);
                if (step == null || SdfFill.y(c) < 0 || body.contains(SdfFill.offset(c, step[0], 0, step[1]))) continue;
                shell++;
                boolean air = false;
                for (int k = 1; k <= 3; k++) {
                    EndBlock b = m.get(SdfFill.offset(c, step[0] * k, 0, step[1] * k));
                    if (b == null) air = true;
                    else {
                        if (air && AmberTree.wood(b)) gaps++;
                        break;
                    }
                }
            }
            assertTrue(gaps <= shell * 0.08, "seed " + i + " gaps " + gaps + " of " + shell);
        }
    }

    @Test
    void woodIsOnePiece() {
        for (long i = 0; i < 20; i++) assertEquals(1, pieces(cells(tree(i)), AmberTree::wood).size(), "seed " + i);
    }

    @Test
    void fruitHangsOnBranches() {
        int fruits = 0;
        for (long i = 0; i < 20; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            List<Set<Long>> seeds = pieces(m, AmberTreeTest::isSeed);
            assertTrue(seeds.size() >= 2, "seed " + i);
            for (Set<Long> fruit : seeds.subList(1, seeds.size())) {
                boolean touches = false;
                for (long c : fruit) for (int[] f : FACES)
                    if (AmberTree.wood(m.get(SdfFill.offset(c, f[0], f[1], f[2])))) touches = true;
                assertTrue(touches, "seed " + i);
                fruits++;
            }
        }
        assertTrue(fruits >= 40, "fruits " + fruits);
    }

    /** The widest reach of the seed body from its axis, in one layer or, with layer null, in all. */
    private static double seedReach(Set<Long> body, Integer layer) {
        double r = 0;
        for (long c : body) if (layer == null || SdfFill.y(c) == layer)
            r = Math.max(r, Math.hypot(SdfFill.x(c) + 0.5, SdfFill.z(c) + 0.5));
        return r;
    }

    @Test
    void seedRestsOnTheGroundToItsRim() {
        // The bottom swells out, so at ground level the seed is nearly as wide as anywhere.
        for (long i = 0; i < 100; i++) {
            Set<Long> body = body(cells(tree(i)));
            double ground = seedReach(body, 0), widest = seedReach(body, null);
            assertTrue(ground >= widest * 0.85, "seed " + i + " " + ground + " of " + widest);
        }
    }

    @Test
    void legsDiveIntoTheGroundOpeningOut() {
        for (long i = 0; i < 50; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            double[] atFoot = { 0, 0 }, deep = { 0, 0 };
            for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
                if (!AmberTree.wood(e.getValue())) continue;
                long c = e.getKey();
                double r = Math.hypot(SdfFill.x(c) + 0.5, SdfFill.z(c) + 0.5);
                double[] into = SdfFill.y(c) == 0 ? atFoot : SdfFill.y(c) == -AmberTree.SINK ? deep : null;
                if (into == null) continue;
                into[0] += r;
                into[1]++;
            }
            assertTrue(deep[1] > 0, "seed " + i + " has no roots");
            assertTrue(deep[0] / deep[1] >= atFoot[0] / atFoot[1] + 0.5, "seed " + i);
        }
    }

    @Test
    void noRootsCrawlOverTheGround() {
        // The roots are the legs going on underground; nothing runs out over the ground past the seed's rim.
        for (long i = 0; i < 100; i++) {
            Map<Long, EndBlock> m = cells(tree(i));
            double rim = seedReach(body(m), 0);
            for (Map.Entry<Long, EndBlock> e : m.entrySet()) {
                long c = e.getKey();
                if (SdfFill.y(c) != 0 || !AmberTree.wood(e.getValue())) continue;
                double r = Math.hypot(SdfFill.x(c) + 0.5, SdfFill.z(c) + 0.5);
                assertTrue(r <= rim + 3, "seed " + i + " wood at " + r + " past rim " + rim);
            }
        }
    }

    @Test
    void legsKeepTheirThicknessDownToTheirFoot() {
        // Ground seven below the root sinks the legs to -10, under where the shape used to be sampled.
        AmberTree.Ground low = (dx, dz) -> -7;
        for (long i = 0; i < 20; i++) {
            AmberTree t = AmberTree.of(seedOf(i), 250, low);
            assertNotNull(t);
            int[] cells = { 0 };
            t.forEach((dx, dy, dz, k) -> { if (dy == -10 && AmberTree.wood(k)) cells[0]++; });
            assertTrue(cells[0] >= 2 * t.strands(), "seed " + i + " " + cells[0] + " cells for " + t.strands());
        }
    }

    @Test
    void staysShallowBesideTheVoid() {
        // The probe reports a lake or the void as -1000; legs and roots must not run down to it.
        AmberTree.Ground edge = (dx, dz) -> dx > 3 ? -1060 : -1;
        for (long i = 0; i < 20; i++) {
            AmberTree t = AmberTree.of(seedOf(i), 250, edge);
            assertNotNull(t);
            assertTrue(t.minY() >= -3 * AmberTree.SINK, "seed " + i + " minY " + t.minY());
        }
    }

    @Test
    void nothingHangsIntoTheVoid() {
        // Past x = 1 there is no ground; a leg's thickness may spill a block or two over the edge.
        AmberTree.Ground edge = (dx, dz) -> dx > 1 ? -1060 : -1;
        int over = 0;
        for (long i = 0; i < 20; i++) {
            final long s = i;
            int[] n = { 0 };
            AmberTree.of(seedOf(i), 250, edge)
                .forEach((dx, dy, dz, k) -> {
                    if (dx < 4 || !AmberTree.wood(k)) return;
                    n[0]++;
                    assertTrue(dy >= -2, "seed " + s + " hangs to " + dy + " at x " + dx);
                });
            over += n[0];
        }
        assertTrue(over > 0, "no wood over the void at all");
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

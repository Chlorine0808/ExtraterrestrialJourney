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

/** BetterEnd's mossy glowshroom at full size: a 36-wide cap on a bent stem, gills below, fur hanging from them. */
class GlowshroomTest {

    private static boolean isFur(EndBlock b) {
        return b == EndBlock.MYCELIA_VINE || b == EndBlock.MYCELIA_VINE_LAST;
    }

    private static Map<Long, EndBlock> cells(Glowshroom g) {
        Map<Long, EndBlock> m = new HashMap<>();
        g.forEach((x, y, z, b) -> m.put(SdfFill.key(x, y, z), b));
        return m;
    }

    /** Spreads small test indices over the seed space; Random's first draws barely move for 1, 2, 3, ... */
    private static long seedOf(long i) {
        return i * 0x9E3779B97F4A7C15L;
    }

    private static Glowshroom normal(long i) {
        Glowshroom g = Glowshroom.of(seedOf(i), 1.0, false, 200);
        assertNotNull(g, "seed " + i);
        return g;
    }

    @Test
    void capWidthAtScaleOne() {
        for (long seed = 1; seed <= 20; seed++) {
            int[] range = { Integer.MAX_VALUE, Integer.MIN_VALUE };
            normal(seed).forEach((x, y, z, b) -> {
                if (b == EndBlock.MYCELIA_CAP) {
                    range[0] = Math.min(range[0], x);
                    range[1] = Math.max(range[1], x);
                }
            });
            int width = range[1] - range[0] + 1;
            // The rim is a thin lip that cell sampling cuts back: about 29 of the nominal 36.
            assertTrue(width >= 26 && width <= 42, "seed " + seed + " width " + width);
        }
    }

    @Test
    void heightIsStemPlusTenAndAHalf() {
        for (long seed = 1; seed <= 20; seed++) {
            Glowshroom g = normal(seed);
            int top = Integer.MIN_VALUE;
            for (var e : cells(g).entrySet()) {
                if (!isFur(e.getValue())) top = Math.max(top, SdfFill.y(e.getKey()));
            }
            // The head sits 11.75 above the stem top; the warp lifts the rim about 0.15 per block of reach.
            int above = top - g.stemLength();
            assertTrue(above >= 12 && above <= 20, "seed " + seed + " above " + above);
            assertEquals(top, g.maxY(), "seed " + seed);
            assertTrue(g.stemLength() >= 10 && g.stemLength() <= 25);
        }
    }

    @Test
    void oneConnectedPiece() {
        for (long seed = 1; seed <= 5; seed++) {
            Map<Long, EndBlock> m = cells(normal(seed));
            Set<Long> body = new HashSet<>();
            m.forEach((k, b) -> { if (!isFur(b)) body.add(k); });
            long start = body.iterator()
                .next();
            Set<Long> seen = new HashSet<>();
            ArrayDeque<Long> open = new ArrayDeque<>();
            seen.add(start);
            open.add(start);
            int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            while (!open.isEmpty()) {
                long c = open.poll();
                for (int[] f : faces) {
                    long n = SdfFill.key(SdfFill.x(c) + f[0], SdfFill.y(c) + f[1], SdfFill.z(c) + f[2]);
                    if (body.contains(n) && seen.add(n)) open.add(n);
                }
            }
            assertEquals(body.size(), seen.size(), "seed " + seed);
        }
    }

    /** Non-fur cells per column, in the order of their height. */
    private static Map<Long, List<int[]>> columns(Glowshroom g) {
        Map<Long, List<int[]>> cols = new HashMap<>();
        g.forEach((x, y, z, b) -> {
            if (isFur(b)) return;
            cols.computeIfAbsent(SdfFill.key(x, 0, z), k -> new ArrayList<>())
                .add(new int[] { y, b.ordinal() });
        });
        cols.values()
            .forEach(l -> l.sort((a, c) -> Integer.compare(a[0], c[0])));
        return cols;
    }

    @Test
    void capOnTop() {
        for (long seed = 1; seed <= 10; seed++) {
            for (List<int[]> col : columns(normal(seed)).values()) {
                int top = col.get(col.size() - 1)[1];
                assertEquals(EndBlock.MYCELIA_CAP.ordinal(), top, "seed " + seed);
            }
        }
    }

    @Test
    void gillsUnderneath() {
        int capColumns = 0, gilled = 0;
        for (long seed = 1; seed <= 10; seed++) {
            for (List<int[]> col : columns(normal(seed)).values()) {
                boolean cap = false, stem = false;
                for (int[] c : col) {
                    cap |= c[1] == EndBlock.MYCELIA_CAP.ordinal();
                    stem |= c[1] == EndBlock.MYCELIA_STEM.ordinal();
                }
                if (!cap || stem) continue;
                capColumns++;
                if (col.get(0)[1] == EndBlock.MYCELIA_GILL.ordinal()) gilled++;
            }
        }
        // Gills reach 12.5 out and the cap 15.6, and over much of the shell the cap is the lower of the two.
        assertTrue(gilled >= 0.3 * capColumns, gilled + " of " + capColumns);
    }

    @Test
    void furHangsFromGills() {
        int runs = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Map<Long, EndBlock> m = cells(normal(seed));
            for (var e : m.entrySet()) {
                if (!isFur(e.getValue())) continue;
                long k = e.getKey();
                int x = SdfFill.x(k), y = SdfFill.y(k), z = SdfFill.z(k);
                EndBlock above = m.get(SdfFill.key(x, y + 1, z));
                if (isFur(above)) continue;
                // The top of a run: a gill sits right above it.
                assertEquals(EndBlock.MYCELIA_GILL, above, "seed " + seed);
                int len = 0;
                while (isFur(m.get(SdfFill.key(x, y - len, z)))) len++;
                assertTrue(len >= 1 && len <= 3, "run " + len);
                assertEquals(EndBlock.MYCELIA_VINE_LAST, m.get(SdfFill.key(x, y - len + 1, z)));
                for (int i = 0; i < len - 1; i++) {
                    assertEquals(EndBlock.MYCELIA_VINE, m.get(SdfFill.key(x, y - i, z)));
                }
                runs++;
            }
        }
        assertTrue(runs > 0);
    }

    @Test
    void stemCarriesBelowTheRoot() {
        boolean found = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    found |= cells(normal(3)).get(SdfFill.key(dx, -Glowshroom.SINK + dy, dz)) == EndBlock.MYCELIA_STEM;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void sameSeedSameCells() {
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        normal(7).forEach((x, y, z, k) -> a.add(x + "," + y + "," + z + k));
        normal(7).forEach((x, y, z, k) -> b.add(x + "," + y + "," + z + k));
        assertEquals(a, b);
    }

    @Test
    void fitsUnderTheLimit() {
        Glowshroom g = Glowshroom.of(seedOf(11), 3.5, true, 60);
        assertNotNull(g);
        assertTrue(g.maxY() <= 60, "maxY " + g.maxY());
        assertTrue(g.scale() < 3.5);
        assertNull(Glowshroom.of(seedOf(11), 3.5, true, 5));
    }

    @Test
    void giantIsBigger() {
        Glowshroom g = Glowshroom.of(seedOf(5), 2.0, true, 200);
        assertNotNull(g);
        int[] range = { Integer.MAX_VALUE, Integer.MIN_VALUE };
        g.forEach((x, y, z, b) -> {
            if (b == EndBlock.MYCELIA_CAP) {
                range[0] = Math.min(range[0], x);
                range[1] = Math.max(range[1], x);
            }
        });
        assertTrue(range[1] - range[0] + 1 >= 60, "width " + (range[1] - range[0] + 1));
    }
}

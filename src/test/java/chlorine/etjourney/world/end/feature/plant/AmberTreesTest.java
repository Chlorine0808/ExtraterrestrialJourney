package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** AMBER seed trees placed per cell from the seed alone and drawn chunk by chunk into air. */
class AmberTreesTest {

    private static AmberTrees.Terrain flat(final double ground) {
        return new AmberTrees.Terrain() {

            @Override
            public double ground(double x, double z) {
                return ground;
            }

            @Override
            public boolean amber(double x, double z) {
                return x >= 0;
            }
        };
    }

    @Test
    void sameSeedSamePlaces() {
        AmberTrees.Terrain t = flat(60);
        for (int c = 0; c < 10; c++) {
            List<AmberTrees.Anchor> a = AmberTrees.in(7L, 3 + c, 4, t), b = AmberTrees.in(7L, 3 + c, 4, t);
            assertEquals(a.size(), b.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(a.get(i).x, b.get(i).x);
                assertEquals(a.get(i).y, b.get(i).y);
                assertEquals(a.get(i).z, b.get(i).z);
                assertEquals(a.get(i).order, b.get(i).order);
            }
        }
    }

    @Test
    void aboutThirtyApart() {
        AmberTrees.Terrain t = flat(60);
        List<int[]> all = new ArrayList<>();
        for (int i = 0; i < 400; i++)
            for (AmberTrees.Anchor a : AmberTrees.in(11L, 1 + i % 20, i / 20, t)) all.add(new int[] { a.x, a.z });
        assertEquals(0.75, all.size() / 400.0, 0.1);
        List<Double> nearest = new ArrayList<>();
        for (int[] p : all) {
            double best = Double.MAX_VALUE;
            for (int[] q : all) if (p != q) best = Math.min(best, Math.hypot(p[0] - q[0], p[1] - q[1]));
            nearest.add(best);
        }
        Collections.sort(nearest);
        double median = nearest.get(nearest.size() / 2);
        assertTrue(median >= 20 && median <= 40, "median " + median);
    }

    @Test
    void onlyOnAmber() {
        AmberTrees.Terrain t = flat(60);
        int seen = 0;
        for (int cx = -5; cx < 5; cx++)
            for (int cz = 0; cz < 3; cz++) for (AmberTrees.Anchor a : AmberTrees.in(5L, cx, cz, t)) {
                assertTrue(a.x >= 0);
                seen++;
            }
        assertTrue(seen > 0);
    }

    @Test
    void highGroundFits() {
        AmberTrees.Terrain t = flat(235);
        int seen = 0;
        for (int i = 0; i < 40; i++) for (AmberTrees.Anchor a : AmberTrees.in(9L, 1 + i, 0, t)) {
            assertTrue(a.y + a.shape.maxY() <= 250);
            seen++;
        }
        assertTrue(seen > 0);
    }

    /** What the nine chunks around (cx, cz) would draw, merged into one ordered list. */
    private static List<AmberTrees.Anchor> around(long seed, int cx, int cz, AmberTrees.Terrain t) {
        Set<String> seen = new HashSet<>();
        List<AmberTrees.Anchor> all = new ArrayList<>();
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) for (AmberTrees.Anchor a : AmberTrees.reaching(seed, cx + i, cz + j, t))
                if (seen.add(a.x + "," + a.z + "," + a.order)) all.add(a);
        Collections.sort(all, (a, b) -> {
            int c = Long.compare(a.order, b.order);
            if (c != 0) return c;
            c = Integer.compare(a.x, b.x);
            return c != 0 ? c : Integer.compare(a.z, b.z);
        });
        return all;
    }

    @Test
    void chunksAgreeWithOnePiece() {
        AmberTrees.Terrain t = flat(60);
        int total = 0;
        for (int k = 0; k < 6; k++) {
            int cx = 8 + 2 * k, cz = 9 + k;
            Map<Long, EndBlock> joined = new HashMap<>();
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) AmberTrees.draw(
                AmberTrees.reaching(21L, cx + i, cz + j, t),
                new MapSink(joined, (cx + i) * 16, (cz + j) * 16, 16, 0, 256));
            Map<Long, EndBlock> whole = new HashMap<>();
            AmberTrees.draw(around(21L, cx, cz, t), new MapSink(whole, (cx - 1) * 16, (cz - 1) * 16, 48, 0, 256));
            assertEquals(whole, joined);
            total += joined.size();
        }
        assertTrue(total > 1000, "cells " + total);
    }

    @Test
    void writesOnlyIntoAir() {
        AmberTrees.Terrain t = flat(60);
        Map<Long, EndBlock> cells = new HashMap<>();
        for (int x = 160; x < 240; x++) for (int z = 160; z < 240; z++)
            for (int y = 61; y < 80; y++) cells.put(MapSink.key(x, y, z), EndBlock.STONE);
        Map<Long, EndBlock> before = new HashMap<>(cells);
        for (int i = 10; i < 15; i++) for (int j = 10; j < 15; j++)
            AmberTrees.draw(AmberTrees.reaching(3L, i, j, t), new MapSink(cells, i * 16, j * 16, 16, 0, 256));
        for (Map.Entry<Long, EndBlock> e : before.entrySet()) assertEquals(e.getValue(), cells.get(e.getKey()));
        assertTrue(cells.size() > before.size());
    }

    private static AmberTrees.Anchor anyTree() {
        AmberTrees.Terrain t = flat(60);
        for (int c = 0;; c++) {
            List<AmberTrees.Anchor> a = AmberTrees.in(5L, 2 + c, 3, t);
            if (!a.isEmpty()) return a.get(0);
        }
    }

    @Test
    void keptFarFromReservedAreas() {
        AmberTrees.Anchor a = anyTree();
        assertTrue(AmberTrees.clearOf(a, Collections.<Area>emptyList()));
        assertTrue(AmberTrees.clearOf(a, Collections.singletonList(new Area("hee", a.x + 2000, a.z, 128))));
    }

    @Test
    void droppedWhenTheFadeReachesALegButNotTheRoot() {
        AmberTrees.Anchor a = anyTree();
        double reach = Math.max(a.shape.maxX(), 1);
        boolean found = false;
        for (double d = 100; d < 600 && !found; d += 0.5) {
            List<Area> areas = Collections.singletonList(new Area("hee", a.x + d, a.z, 128));
            if (Reservations.suppression(areas, a.x, a.z) > 0.1) continue;
            if (Reservations.suppression(areas, a.x + reach, a.z) <= 0.1) continue;
            found = true;
            assertFalse(AmberTrees.clearOf(a, areas));
        }
        assertTrue(found);
    }
}

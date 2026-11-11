package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A rasterised polyline holds both endpoints, is one face-connected piece and depends on nothing but its input. */
class BlockLinesTest {

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static List<double[]> spline(double[]... points) {
        List<double[]> out = new ArrayList<>();
        for (double[] p : points) out.add(p);
        return out;
    }

    private static Map<Long, EndBlock> run(List<double[]> spline) {
        Map<Long, EndBlock> cells = new LinkedHashMap<>();
        BlockLines.line(spline, cells, EndBlock.STONE);
        return cells;
    }

    private static long cellOf(double[] p) {
        return SdfFill.key((int) Math.floor(p[0]), (int) Math.floor(p[1]), (int) Math.floor(p[2]));
    }

    private static int reachableFrom(long start, Map<Long, EndBlock> cells) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            long k = queue.poll();
            for (int[] f : FACES) {
                long n = SdfFill.offset(k, f[0], f[1], f[2]);
                if (cells.containsKey(n) && seen.add(n)) queue.add(n);
            }
        }
        return seen.size();
    }

    @Test
    void steepAndFlatSegmentsAreFaceConnected() {
        List<double[]> s = spline(
            new double[] { 0.3, 0.2, 0.9 },
            new double[] { 7.8, 5.1, 3.4 },
            new double[] { 9.2, -6.6, -4.5 },
            new double[] { -3.1, -6.0, 8.8 });
        Map<Long, EndBlock> cells = run(s);
        assertEquals(cells.size(), reachableFrom(cellOf(s.get(0)), cells));
    }

    @Test
    void lineHoldsBothEndpointCells() {
        List<double[]> s = spline(new double[] { -2.5, 10.1, 4.7 }, new double[] { 6.2, 3.3, -8.9 });
        Map<Long, EndBlock> cells = run(s);
        assertTrue(cells.containsKey(cellOf(s.get(0))));
        assertTrue(cells.containsKey(cellOf(s.get(1))));
    }

    @Test
    void sameInputGivesSameCellsInSameOrder() {
        List<double[]> s = spline(new double[] { 0, 0, 0 }, new double[] { 5.5, 2.2, -3.3 });
        assertEquals(new ArrayList<>(run(s).keySet()), new ArrayList<>(run(s).keySet()));
    }

    @Test
    void degenerateSplinesAddNothingOrOneCell() {
        assertTrue(run(spline()).isEmpty());
        assertTrue(run(spline(new double[] { 1, 2, 3 })).isEmpty());
        assertEquals(1, run(spline(new double[] { 1.5, 2.5, 3.5 }, new double[] { 1.5, 2.5, 3.5 })).size());
    }
}

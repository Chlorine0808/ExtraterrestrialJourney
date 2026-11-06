package chlorine.etjourney.world.end.feature.plant;

import java.util.List;
import java.util.Map;

import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** One-block-thick lines of wood along a polyline, shared by the tree shapes. */
final class BlockLines {

    private BlockLines() {}

    /** Puts the block along the polyline, face-connected. */
    static void line(List<double[]> spline, Map<Long, EndBlock> cells, EndBlock block) {
        int[] prev = null;
        for (int i = 0; i + 1 < spline.size(); i++) {
            double[] a = spline.get(i), b = spline.get(i + 1);
            double len = Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
            int steps = Math.max(1, (int) Math.ceil(len * 3));
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                int[] cell = { (int) Math.floor(a[0] + (b[0] - a[0]) * t), (int) Math.floor(a[1] + (b[1] - a[1]) * t),
                    (int) Math.floor(a[2] + (b[2] - a[2]) * t) };
                if (prev != null) {
                    // Step one axis at a time so the line never joins on an edge or a corner only.
                    for (int axis = 0; axis < 3; axis++) {
                        if (prev[axis] == cell[axis]) continue;
                        prev[axis] = cell[axis];
                        cells.put(SdfFill.key(prev[0], prev[1], prev[2]), block);
                    }
                } else {
                    prev = cell.clone();
                }
                cells.put(SdfFill.key(cell[0], cell[1], cell[2]), block);
            }
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}

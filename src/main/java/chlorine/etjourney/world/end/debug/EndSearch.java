package chlorine.etjourney.world.end.debug;

/** Nearest-target searches for the debug commands: true Euclidean nearest within a radius. */
final class EndSearch {

    static final int RADIUS = 8192;
    private static final int STEP = 16;

    private EndSearch() {}

    interface CellProbe {

        /** The cell's target as {x, z, extra...}, or null. */
        double[] at(int cx, int cz);
    }

    interface PointTest {

        boolean hit(double x, double z);
    }

    /** Nearest cell target within RADIUS; a target must stay inside its cell. */
    static double[] nearestInCells(int cell, double ox, double oz, CellProbe probe) {
        int ocx = (int) Math.floor(ox / cell), ocz = (int) Math.floor(oz / cell);
        double[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int ring = 0; ring <= RADIUS / cell + 1; ring++) {
            if ((ring - 1) * cell > Math.min(bestDistance, RADIUS)) break;
            for (int i = -ring; i <= ring; i++) {
                for (int j = -ring; j <= ring; j++) {
                    if (Math.abs(i) != ring && Math.abs(j) != ring) continue;
                    double[] p = probe.at(ocx + i, ocz + j);
                    if (p == null) continue;
                    double d = Math.hypot(p[0] - ox, p[1] - oz);
                    if (d < bestDistance && d <= RADIUS) {
                        bestDistance = d;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    /** Nearest sampled point (every STEP blocks) passing the test within RADIUS. */
    static double[] nearestPoint(double ox, double oz, PointTest test) {
        double[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int r = 0; r <= RADIUS && r <= bestDistance; r += STEP) {
            for (int dx = -r; dx <= r; dx += STEP) {
                for (int dz = -r; dz <= r; dz += STEP) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    double d = Math.hypot(dx, dz);
                    if (d >= bestDistance || d > RADIUS || !test.hit(ox + dx, oz + dz)) continue;
                    bestDistance = d;
                    best = new double[] { ox + dx, oz + dz };
                }
            }
        }
        return best;
    }
}

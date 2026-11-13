package chlorine.etjourney.content.end.wormcheck;

import java.util.Locale;

/** Gaps between neighbouring centres of a worm, head first. */
final class SegmentGaps {

    /** Chords may be shorter than the spacing on a bend, never much longer. */
    private static final double MIN = 0.5, MAX = 1.05;

    private SegmentGaps() {}

    static boolean trail(double[][] centres, double spacing) {
        for (int i = 1; i < centres.length; i++) {
            double gap = gap(centres[i - 1], centres[i]);
            if (gap < MIN * spacing || gap > MAX * spacing) return false;
        }
        return true;
    }

    static String describe(double[][] centres) {
        double min = Double.MAX_VALUE, max = 0;
        for (int i = 1; i < centres.length; i++) {
            double gap = gap(centres[i - 1], centres[i]);
            min = Math.min(min, gap);
            max = Math.max(max, gap);
        }
        return String.format(Locale.ROOT, "gaps %.2f to %.2f", min, max);
    }

    private static double gap(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

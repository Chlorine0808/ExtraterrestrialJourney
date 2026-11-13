package chlorine.etjourney.content.end.wormcheck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The worm check's verdict on whether the segments trail the head instead of bunching or tearing away. */
class SegmentGapsTest {

    private static double[][] line(double... xs) {
        double[][] c = new double[xs.length][];
        for (int i = 0; i < xs.length; i++) c[i] = new double[] { xs[i], 0, 0 };
        return c;
    }

    @Test
    void evenlySpacedSegmentsTrail() {
        assertTrue(SegmentGaps.trail(line(0, 1, 2, 3, 4), 1));
    }

    @Test
    void aCurvedBodyWithShorterChordsStillTrails() {
        assertTrue(SegmentGaps.trail(line(0, 0.8, 1.5, 2.2), 1));
    }

    @Test
    void segmentsBunchedOnTheHeadDoNotTrail() {
        assertFalse(SegmentGaps.trail(line(0, 0, 0, 0), 1));
    }

    @Test
    void aSegmentTornAwayDoesNotTrail() {
        assertFalse(SegmentGaps.trail(line(0, 1, 2, 5), 1));
    }
}

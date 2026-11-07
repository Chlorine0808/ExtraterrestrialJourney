package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class FurFacingTest {

    /** The faces holding a leaf, as offsets from the fur. */
    private static int[] outward(int[]... leaves) {
        List<int[]> at = Arrays.asList(leaves);
        return FurFacing.outward(
            (dx, dy, dz) -> at.stream()
                .anyMatch(l -> l[0] == dx && l[1] == dy && l[2] == dz));
    }

    @Test
    void furOnTopOfALeafStandsUp() {
        assertArrayEquals(new int[] { 0, 1, 0 }, outward(new int[] { 0, -1, 0 }));
    }

    @Test
    void furBesideALeafPointsAwayFromIt() {
        assertArrayEquals(new int[] { 1, 0, 0 }, outward(new int[] { -1, 0, 0 }));
        assertArrayEquals(new int[] { 0, 0, -1 }, outward(new int[] { 0, 0, 1 }));
    }

    @Test
    void aLeafBelowWinsOverOneBeside() {
        assertArrayEquals(new int[] { 0, 1, 0 }, outward(new int[] { 1, 0, 0 }, new int[] { 0, -1, 0 }));
    }

    @Test
    void furUnderALeafHangsDown() {
        assertArrayEquals(new int[] { 0, -1, 0 }, outward(new int[] { 0, 1, 0 }));
    }

    @Test
    void noLeafStandsUp() {
        assertArrayEquals(new int[] { 0, 1, 0 }, outward());
    }
}

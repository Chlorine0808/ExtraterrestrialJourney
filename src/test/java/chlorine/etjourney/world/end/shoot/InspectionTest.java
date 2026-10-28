package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class InspectionTest {

    private static int[] fill(int w, int h, int argb) {
        int[] p = new int[w * h];
        Arrays.fill(p, argb);
        return p;
    }

    private static int randomColour(Random r) {
        return 0xFF000000 | r.nextInt(0x1000000);
    }

    private static int[] skyOverNoise(int w, int h) {
        Random r = new Random(1);
        int[] p = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) p[y * w + x] = y < h * 0.4 ? 0xFF6080C0 : randomColour(r);
        }
        return p;
    }

    private static int[] noiseWithFlatCentre(int w, int h) {
        Random r = new Random(1);
        int[] p = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean centre = x >= w / 3 && x < 2 * w / 3 && y >= h / 3 && y < 2 * h / 3;
                p[y * w + x] = centre ? 0xFF6080C0 : randomColour(r);
            }
        }
        return p;
    }

    private static int[] darkNoise(int w, int h) {
        Random r = new Random(1);
        int[] p = new int[w * h];
        for (int i = 0; i < p.length; i++) p[i] = 0xFF000000 | r.nextInt(16) << 16 | r.nextInt(16) << 8 | r.nextInt(16);
        return p;
    }

    @Test
    void uniformImageFails() {
        assertEquals(Inspection.Fault.UNIFORM, Inspection.inspect(fill(1920, 1080, 0xFF1A1020), 1920, 1080));
    }

    @Test
    void landscapePasses() {
        assertEquals(Inspection.Fault.NONE, Inspection.inspect(skyOverNoise(1920, 1080), 1920, 1080));
    }

    @Test
    void blankCentreFails() {
        assertEquals(Inspection.Fault.EMPTY_CENTRE, Inspection.inspect(noiseWithFlatCentre(1920, 1080), 1920, 1080));
    }

    @Test
    void darkImageFails() {
        assertEquals(Inspection.Fault.DARK, Inspection.inspect(darkNoise(1920, 1080), 1920, 1080));
    }

    @Test
    void tooSmallImageFails() {
        assertEquals(Inspection.Fault.SMALL, Inspection.inspect(skyOverNoise(1536, 864), 1536, 864));
    }
}

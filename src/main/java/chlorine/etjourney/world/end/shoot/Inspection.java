package chlorine.etjourney.world.end.shoot;

/** Rejects screenshots that are too small, one flat colour, empty in the centre, or too dark. */
public final class Inspection {

    static final int MIN_WIDTH = 1920, MIN_HEIGHT = 1080;

    public enum Fault {
        NONE,
        SMALL,
        UNIFORM,
        EMPTY_CENTRE,
        DARK
    }

    private static final int STEP = 8;

    private Inspection() {}

    static Fault inspect(int[] argb, int w, int h) {
        if (w < MIN_WIDTH || h < MIN_HEIGHT) return Fault.SMALL;
        int[] all = new int[512], centre = new int[512];
        int nAll = 0, nCentre = 0;
        double lum = 0;
        for (int y = 0; y < h; y += STEP) {
            for (int x = 0; x < w; x += STEP) {
                int c = argb[y * w + x];
                int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                int bin = r >> 5 << 6 | g >> 5 << 3 | b >> 5;
                all[bin]++;
                nAll++;
                lum += 0.299 * r + 0.587 * g + 0.114 * b;
                if (x >= w / 3 && x < 2 * w / 3 && y >= h / 3 && y < 2 * h / 3) {
                    centre[bin]++;
                    nCentre++;
                }
            }
        }
        // Darkness first: a near-black image also lands in one colour bin.
        if (lum / nAll < 12) return Fault.DARK;
        if (max(all) > 0.8 * nAll) return Fault.UNIFORM;
        if (nCentre > 0 && max(centre) > 0.97 * nCentre) return Fault.EMPTY_CENTRE;
        return Fault.NONE;
    }

    private static int max(int[] a) {
        int m = 0;
        for (int v : a) m = Math.max(m, v);
        return m;
    }
}

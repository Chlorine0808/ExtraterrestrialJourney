package chlorine.etjourney.world.end.shoot;

/** Decides that rendering has settled once consecutive thumbnails stop changing. */
public final class Stability {

    private final int needed;
    private final double tolerance;
    private int[] previous;
    private int quiet;

    public Stability(int needed, double tolerance) {
        this.needed = needed;
        this.tolerance = tolerance;
    }

    public boolean offer(int[] thumb) {
        if (previous != null && previous.length == thumb.length) {
            long sum = 0;
            for (int i = 0; i < thumb.length; i++) {
                int a = previous[i], b = thumb[i];
                sum += Math.abs((a >> 16 & 255) - (b >> 16 & 255)) + Math.abs((a >> 8 & 255) - (b >> 8 & 255))
                    + Math.abs((a & 255) - (b & 255));
            }
            double mean = thumb.length == 0 ? 0 : sum / (3.0 * thumb.length);
            quiet = mean <= tolerance ? quiet + 1 : 0;
        } else {
            quiet = 0;
        }
        previous = thumb;
        return quiet >= needed;
    }

    /** Box-averages the image down to tw x th (each thumbnail pixel covers a block of source pixels). */
    public static int[] thumbnail(int[] argb, int w, int h, int tw, int th) {
        int[] out = new int[tw * th];
        for (int ty = 0; ty < th; ty++) {
            int y0 = ty * h / th, y1 = Math.max(y0 + 1, (ty + 1) * h / th);
            for (int tx = 0; tx < tw; tx++) {
                int x0 = tx * w / tw, x1 = Math.max(x0 + 1, (tx + 1) * w / tw);
                long r = 0, g = 0, b = 0;
                int n = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int c = argb[y * w + x];
                        r += c >> 16 & 255;
                        g += c >> 8 & 255;
                        b += c & 255;
                        n++;
                    }
                }
                out[ty * tw + tx] = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            }
        }
        return out;
    }
}

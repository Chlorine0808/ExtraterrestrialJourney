package chlorine.etjourney.world.end.noise;

/**
 * 2D OpenSimplex noise in [-1, 1], ported from the 2D part of BetterEnd's {@code OpenSimplexNoise} (Kurt Spencer,
 * public domain), so a shape that samples it keeps upstream's spread of values.
 */
public final class OpenSimplex2D {

    private static final double STRETCH = -0.211324865405187;
    private static final double SQUISH = 0.366025403784439;
    private static final double NORM = 47;
    private static final byte[] GRADIENTS = { 5, 2, 2, 5, -5, 2, -2, 5, 5, -2, 2, -5, -5, -2, -2, -5 };

    private final short[] perm = new short[256];

    public OpenSimplex2D(long seed) {
        short[] source = new short[256];
        for (short i = 0; i < 256; i++) source[i] = i;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        for (int i = 255; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((seed + 31) % (i + 1));
            if (r < 0) r += i + 1;
            perm[i] = source[r];
            source[r] = source[i];
        }
    }

    public double eval(double x, double y) {
        double stretch = (x + y) * STRETCH;
        double xs = x + stretch, ys = y + stretch;
        int xsb = floor(xs), ysb = floor(ys);
        double squish = (xsb + ysb) * SQUISH;
        double xb = xsb + squish, yb = ysb + squish;
        double xins = xs - xsb, yins = ys - ysb;
        double inSum = xins + yins;
        double dx0 = x - xb, dy0 = y - yb;
        double dxExt, dyExt;
        int xsvExt, ysvExt;
        double value = 0;

        double dx1 = dx0 - 1 - SQUISH, dy1 = dy0 - SQUISH;
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb + 1, ysb, dx1, dy1);
        }
        double dx2 = dx0 - SQUISH, dy2 = dy0 - 1 - SQUISH;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb, ysb + 1, dx2, dy2);
        }

        if (inSum <= 1) {
            double zins = 1 - inSum;
            if (zins > xins || zins > yins) {
                if (xins > yins) {
                    xsvExt = xsb + 1;
                    ysvExt = ysb - 1;
                    dxExt = dx0 - 1;
                    dyExt = dy0 + 1;
                } else {
                    xsvExt = xsb - 1;
                    ysvExt = ysb + 1;
                    dxExt = dx0 + 1;
                    dyExt = dy0 - 1;
                }
            } else {
                xsvExt = xsb + 1;
                ysvExt = ysb + 1;
                dxExt = dx0 - 1 - 2 * SQUISH;
                dyExt = dy0 - 1 - 2 * SQUISH;
            }
        } else {
            double zins = 2 - inSum;
            if (zins < xins || zins < yins) {
                if (xins > yins) {
                    xsvExt = xsb + 2;
                    ysvExt = ysb;
                    dxExt = dx0 - 2 - 2 * SQUISH;
                    dyExt = dy0 - 2 * SQUISH;
                } else {
                    xsvExt = xsb;
                    ysvExt = ysb + 2;
                    dxExt = dx0 - 2 * SQUISH;
                    dyExt = dy0 - 2 - 2 * SQUISH;
                }
            } else {
                dxExt = dx0;
                dyExt = dy0;
                xsvExt = xsb;
                ysvExt = ysb;
            }
            xsb += 1;
            ysb += 1;
            dx0 = dx0 - 1 - 2 * SQUISH;
            dy0 = dy0 - 1 - 2 * SQUISH;
        }

        double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
        }
        double attnExt = 2 - dxExt * dxExt - dyExt * dyExt;
        if (attnExt > 0) {
            attnExt *= attnExt;
            value += attnExt * attnExt * extrapolate(xsvExt, ysvExt, dxExt, dyExt);
        }
        return value / NORM;
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        int index = perm[(perm[xsb & 0xFF] + ysb) & 0xFF] & 0x0E;
        return GRADIENTS[index] * dx + GRADIENTS[index + 1] * dy;
    }

    private static int floor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}

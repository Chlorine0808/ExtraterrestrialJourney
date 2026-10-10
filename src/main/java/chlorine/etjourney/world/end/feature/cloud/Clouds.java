package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Fade;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** Shared limits and placement rules of the cloud structures. */
final class Clouds {

    /** Highest block any cloud reaches. */
    static final double CEILING = 248;
    /** Clearance a floating cloud keeps over the ground, and its lowest height over the void. */
    static final double OVER_GROUND = 30, VOID_FLOOR = 40;

    private Clouds() {}

    /** Whether the cloud of a cell forms at its style's weight, thinning out across the border. */
    static boolean forms(long s, int cx, int cz, double weight) {
        return Fade.forms(weight, Hash.hash01(s + 99, cx, cz));
    }

    /** A position in cell c, kept margin of the cell away from both of its edges. */
    static double place(int c, int cell, double margin, double roll) {
        return (c + margin + (1 - 2 * margin) * roll) * cell;
    }

    /** Lowest Y the bottom of a floating cloud may sit at over (x, z). */
    static double floor(StructureProbe probe, double x, double z) {
        double ground = probe.ground(x, z);
        return ground > -100 ? ground + OVER_GROUND : VOID_FLOOR;
    }

    /** roll of the way from low to high, or NaN when low is above high. */
    static double between(double low, double high, double roll) {
        return low > high ? Double.NaN : low + (high - low) * roll;
    }

    /** Like max, but rounds the seam where a and b lie within k of each other. */
    static double smoothMax(double a, double b, double k) {
        double h = Math.max(k - Math.abs(a - b), 0) / k;
        return Math.max(a, b) + h * h * k * 0.25;
    }
}

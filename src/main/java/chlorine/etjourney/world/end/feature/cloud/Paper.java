package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** STRATUS: a sharp-edged sheet one block thick, tilted and gently waving, like a drifting piece of paper. */
public final class Paper extends Structure {

    private static final long SALT = 0x4E9A2C7F1B3D5068L;
    /** Steepest tilt along the paper's length and across it. */
    private static final double MAX_PITCH = Math.toRadians(25), MAX_ROLL = Math.toRadians(12);

    public static final Structure.Kind<Paper> KIND = new Structure.Kind<Paper>("STRATUS", 16, 18) {

        @Override
        protected Paper compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ SALT;
            // Crowded in some places and nearly empty in others.
            double crowd = ValueNoise.mask(s + 20, cx * (double) cell, cz * (double) cell, 180);
            if (Hash.hash01(s, cx, cz) > 0.05 + 0.6 * crowd * crowd) return null;
            double x = Clouds.place(cx, cell, 0.15, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.15, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double length = 8 + 16 * Hash.hash01(s + 3, cx, cz);
            double width = length * (0.5 + 0.4 * Hash.hash01(s + 4, cx, cz));
            double yaw = Hash.hash01(s + 5, cx, cz) * Math.PI * 2;
            double pitch = (2 * Hash.hash01(s + 6, cx, cz) - 1) * MAX_PITCH;
            double roll = (2 * Hash.hash01(s + 7, cx, cz) - 1) * MAX_ROLL;
            double amp = 0.5 + 1.5 * Hash.hash01(s + 8, cx, cz);
            double wave = 10 + 14 * Hash.hash01(s + 9, cx, cz);
            double phase = Hash.hash01(s + 10, cx, cz) * Math.PI * 2;
            double reach = reach(length, width, Math.tan(pitch), Math.tan(roll), amp);
            double ground = probe.ground(x, z), height = Hash.hash01(s + 11, cx, cz);
            double y = ground > -100
                ? Clouds.between(ground + 4 + reach, Math.min(ground + 90, Clouds.CEILING - reach), height)
                : Clouds.between(60, 200, height);
            return Double.isNaN(y) ? null
                : new Paper(x, z, y, length, width, yaw, Math.tan(pitch), Math.tan(roll), amp, wave, phase);
        }
    };

    public final double centreY, length, width;
    private final double yaw, tanPitch, tanRoll, amp, k, phase;

    Paper(double x, double z, double y, double length, double width, double yaw, double tanPitch, double tanRoll,
        double amp, double wave, double phase) {
        super(
            x,
            z,
            Math.hypot(length / 2, width / 2) + 1,
            y - reach(length, width, tanPitch, tanRoll, amp) - 1,
            y + reach(length, width, tanPitch, tanRoll, amp) + 1);
        this.centreY = y;
        this.length = length;
        this.width = width;
        this.yaw = yaw;
        this.tanPitch = tanPitch;
        this.tanRoll = tanRoll;
        this.amp = amp;
        this.k = 2 * Math.PI / wave;
        this.phase = phase;
    }

    /** Farthest the sheet rises or sinks from its centre height. */
    private static double reach(double length, double width, double tanPitch, double tanRoll, double amp) {
        return length / 2 * Math.abs(tanPitch) + width / 2 * Math.abs(tanRoll) + amp + 1;
    }

    /** World {x, y, z} of the sheet's surface at u along its length and w across it, from its centre. */
    public double[] at(double u, double w) {
        double c = Math.cos(yaw), s = Math.sin(yaw);
        return new double[] { centreX + u * c - w * s, surface(u, w), centreZ + u * s + w * c };
    }

    private double surface(double u, double w) {
        return centreY + u * tanPitch + w * tanRoll + amp * Math.sin(k * u + phase);
    }

    @Override
    protected double body(double x, double y, double z) {
        double dx = x - centreX, dz = z - centreZ, c = Math.cos(yaw), s = Math.sin(yaw);
        double u = dx * c + dz * s, w = -dx * s + dz * c;
        // Cut square at the edges.
        if (Math.abs(u) > length / 2 || Math.abs(w) > width / 2) return -1;
        double slope = Math.abs(tanPitch + amp * k * Math.cos(k * u + phase)) + Math.abs(tanRoll);
        // Half a block, widened on slopes so neighbouring columns of the sheet still touch.
        return 0.5 * (1 + slope) - Math.abs(y - surface(u, w));
    }
}

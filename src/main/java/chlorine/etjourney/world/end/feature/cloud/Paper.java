package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** STRATUS: a sharp-edged sheet one block thick with nibbled edges, tilted and gently waving, like drifting paper. */
public final class Paper extends Structure {

    private static final long SALT = 0x4E9A2C7F1B3D5068L;
    /** Steepest tilt along the paper's length and across it. */
    private static final double MAX_PITCH = Math.toRadians(25), MAX_ROLL = Math.toRadians(12);
    /** Deepest bite into the edges, as a share of the half length or width. */
    private static final double NIBBLE = 0.25;

    public static final Structure.Kind<Paper> KIND = new Structure.Kind<Paper>("STRATUS", 16, 23) {

        @Override
        protected Paper compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ SALT;
            // Crowded in some places and nearly empty in others.
            double crowd = ValueNoise.mask(s + 20, cx * (double) cell, cz * (double) cell, 180);
            if (Hash.hash01(s, cx, cz) > 0.05 + 0.6 * crowd * crowd) return null;
            double x = Clouds.place(cx, cell, 0.15, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.15, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            // Mostly short scraps, now and then a long strip.
            double length = 6 + 34 * Math.pow(Hash.hash01(s + 3, cx, cz), 1.5);
            double width = Math.min(length, 20) * (0.35 + 0.55 * Hash.hash01(s + 4, cx, cz));
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
                : new Paper(
                    x,
                    z,
                    y,
                    length,
                    width,
                    yaw,
                    Math.tan(pitch),
                    Math.tan(roll),
                    amp,
                    wave,
                    phase,
                    s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double centreY, length, width;
    private final double yaw, tanPitch, tanRoll, amp, k, phase;
    private final long salt;

    Paper(double x, double z, double y, double length, double width, double yaw, double tanPitch, double tanRoll,
        double amp, double wave, double phase, long salt) {
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
        this.salt = salt;
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
        // Cut square at the edges, with bites taken out of them.
        double reach = Math.max(Math.abs(u) / (length / 2), Math.abs(w) / (width / 2));
        double bite = Math.max(0, Math.min(1, (ValueNoise.mask(salt, x, z, 3) - 0.3) / 0.4));
        if (reach > 1 - NIBBLE * bite) return -1;
        double slope = Math.abs(tanPitch + amp * k * Math.cos(k * u + phase)) + Math.abs(tanRoll);
        // Half a block, widened on slopes so neighbouring columns of the sheet still touch.
        return 0.5 * (1 + slope) - Math.abs(y - surface(u, w));
    }
}

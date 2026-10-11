package chlorine.etjourney.world.end.noise;

/** Planar distance without Math.hypot, which Java 8 computes slowly in software. */
public final class Distance {

    private Distance() {}

    public static double planar(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }
}

package chlorine.etjourney.content.projectile;

/** How a shot is launched at a point: straight at it, or in an arc that comes down on it. */
public enum Aim {

    STRAIGHT,
    LOB;

    /** Velocity towards (dx, dy, dz) at the given speed; forward along z when the point is the origin. */
    public static double[] straight(double dx, double dy, double dz, double speed) {
        double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (n < 1e-9) return new double[] { 0, 0, speed };
        return new double[] { dx / n * speed, dy / n * speed, dz / n * speed };
    }

    /**
     * Velocity for a shot that moves and then loses {@code gravity} each tick, peaks {@code arcHeight} above the
     * higher of its start and the target, and comes down at the target.
     */
    public static double[] lob(double dx, double dy, double dz, double arcHeight, double gravity) {
        double peak = Math.max(0, dy) + arcHeight;
        // Moving before the pull makes the discrete arc the continuous one launched half a pull faster.
        double rise = Math.sqrt(2 * gravity * peak);
        double airborne = rise / gravity + Math.sqrt(2 * (peak - dy) / gravity);
        double vy = rise - gravity / 2;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h < 1e-9) return new double[] { 0, vy, 0 };
        double speed = h / airborne;
        return new double[] { dx / h * speed, vy, dz / h * speed };
    }
}

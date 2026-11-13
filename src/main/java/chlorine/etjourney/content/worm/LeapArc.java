package chlorine.etjourney.content.worm;

/** The launch velocity of a leap that peaks at a height and lands a distance away. */
public final class LeapArc {

    private LeapArc() {}

    /**
     * Velocity per tick for a body that moves and then loses {@code gravity} each tick.
     *
     * @param dirX     horizontal direction, any length
     * @param distance horizontal distance to the point where it is back at its starting height
     * @return {vx, vy, vz}
     */
    public static double[] launch(double dirX, double dirZ, double distance, double apex, double gravity) {
        // Moving before the pull makes the discrete arc the continuous one launched half a pull faster.
        double rise = Math.sqrt(2 * gravity * apex);
        double vy = rise - gravity / 2;
        double airborne = 2 * rise / gravity;
        double n = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (n < 1e-9) return new double[] { 0, vy, 0 };
        double speed = distance / airborne;
        return new double[] { dirX / n * speed, vy, dirZ / n * speed };
    }
}

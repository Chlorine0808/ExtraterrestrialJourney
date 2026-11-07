package chlorine.etjourney.world.updraft.model;

/**
 * A box of moving air: each tick it adds lift to the speed of what is inside along its direction, up to maxSpeed.
 * Straight up unless given another direction.
 */
public final class Updraft {

    public final double minX, minY, minZ, maxX, maxY, maxZ;
    /** Unit vector the air moves along. */
    public final double dirX, dirY, dirZ;
    /** Speed added each tick along the direction, in blocks per tick. */
    public final double lift;
    /** Speed along the direction the lift stops at, in blocks per tick. */
    public final double maxSpeed;

    public Updraft(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double lift,
        double maxSpeed) {
        this(minX, minY, minZ, maxX, maxY, maxZ, 0, 1, 0, lift, maxSpeed);
    }

    /** The direction may have any length; it is normalized. */
    public Updraft(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double dirX,
        double dirY, double dirZ, double lift, double maxSpeed) {
        if (!(minX < maxX && minY < maxY && minZ < maxZ)) throw new IllegalArgumentException("Empty updraft box");
        if (!(lift > 0 && maxSpeed > 0)) throw new IllegalArgumentException("Lift and max speed must be positive");
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (!(length > 0)) throw new IllegalArgumentException("Updraft needs a direction");
        this.dirX = dirX / length;
        this.dirY = dirY / length;
        this.dirZ = dirZ / length;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.lift = lift;
        this.maxSpeed = maxSpeed;
    }

    /** True when the box shares volume with this one; touching faces do not count. */
    public boolean overlaps(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return minX < this.maxX && maxX > this.minX
            && minY < this.maxY
            && maxY > this.minY
            && minZ < this.maxZ
            && maxZ > this.minZ;
    }

    /** Motion after one tick in this updraft; only the part along the direction changes. */
    public double[] push(double motionX, double motionY, double motionZ) {
        double along = motionX * dirX + motionY * dirY + motionZ * dirZ;
        double add = along >= maxSpeed ? 0 : Math.min(lift, maxSpeed - along);
        return new double[] { motionX + add * dirX, motionY + add * dirY, motionZ + add * dirZ };
    }

    /** True when the air has an upward part, so what it carries takes no fall damage. */
    public boolean breaksFall() {
        return dirY > 0;
    }

    /** The updraft with the highest cap, then the most lift, among those overlapping the box; null if none. */
    public static Updraft strongest(Iterable<Updraft> updrafts, double minX, double minY, double minZ, double maxX,
        double maxY, double maxZ) {
        Updraft best = null;
        for (Updraft u : updrafts) {
            if (!u.overlaps(minX, minY, minZ, maxX, maxY, maxZ)) continue;
            if (best == null || u.maxSpeed > best.maxSpeed || (u.maxSpeed == best.maxSpeed && u.lift > best.lift))
                best = u;
        }
        return best;
    }

    @Override
    public String toString() {
        return String.format(
            "[%.1f %.1f %.1f .. %.1f %.1f %.1f] dir %.2f %.2f %.2f lift %.2f max %.2f",
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            dirX,
            dirY,
            dirZ,
            lift,
            maxSpeed);
    }
}

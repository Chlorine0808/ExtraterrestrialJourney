package chlorine.etjourney.world.updraft.model;

/** A box of rising air: each tick it adds lift to the upward speed of what is inside, up to maxSpeed. */
public final class Updraft {

    public final double minX, minY, minZ, maxX, maxY, maxZ;
    /** Upward speed added each tick, in blocks per tick. */
    public final double lift;
    /** Upward speed the lift stops at, in blocks per tick. */
    public final double maxSpeed;

    public Updraft(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double lift,
        double maxSpeed) {
        if (!(minX < maxX && minY < maxY && minZ < maxZ)) throw new IllegalArgumentException("Empty updraft box");
        if (!(lift > 0 && maxSpeed > 0)) throw new IllegalArgumentException("Lift and max speed must be positive");
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

    /** Upward speed after one tick in this updraft. */
    public double push(double motionY) {
        if (motionY >= maxSpeed) return motionY;
        return Math.min(motionY + lift, maxSpeed);
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
            "[%.1f %.1f %.1f .. %.1f %.1f %.1f] lift %.2f max %.2f",
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            lift,
            maxSpeed);
    }
}

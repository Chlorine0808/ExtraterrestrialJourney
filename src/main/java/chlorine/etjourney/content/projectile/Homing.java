package chlorine.etjourney.content.projectile;

/** How a shot turns towards its target: by a capped angle a tick, inside a window of its life. */
public final class Homing {

    /** Radians a tick. */
    public final double turnRate;
    /** Ticks of straight flight before it starts turning. */
    public final int delay;
    /** Ticks it keeps turning; -1 for the rest of its life. */
    public final int duration;
    /** How far it looks for a target when it was given none. */
    public final double range;

    public Homing(double turnRate, int delay, int duration, double range) {
        if (!(turnRate > 0)) throw new IllegalArgumentException("turnRate must be > 0: " + turnRate);
        if (delay < 0) throw new IllegalArgumentException("delay must be >= 0: " + delay);
        if (!(range > 0)) throw new IllegalArgumentException("range must be > 0: " + range);
        this.turnRate = turnRate;
        this.delay = delay;
        this.duration = duration;
        this.range = range;
    }

    public boolean activeAt(int age) {
        return age >= delay && (duration < 0 || age < delay + duration);
    }
}

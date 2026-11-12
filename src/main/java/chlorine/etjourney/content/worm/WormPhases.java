package chlorine.etjourney.content.worm;

/** A worm's swim, warn and leap cycle. */
public final class WormPhases {

    public enum Phase {
        ROAM,
        WARN,
        LEAP
    }

    /** What the worm swims through: the ground, breaking out of it, or the open air. */
    public enum Medium {
        GROUND,
        AIR
    }

    private static final int MAX_DUST = 4;

    private final Medium medium;
    private final int warnTicks, leapTicks, maxLeapTicks, cooldown;
    private final double strikeRange;
    private Phase phase = Phase.ROAM;
    private int elapsed, cooldownLeft;

    public WormPhases(Medium medium, int warnTicks, int leapTicks, int maxLeapTicks, int cooldown, double strikeRange) {
        this.medium = medium;
        this.warnTicks = warnTicks;
        this.leapTicks = leapTicks;
        this.maxLeapTicks = maxLeapTicks;
        this.cooldown = cooldown;
        this.strikeRange = strikeRange;
    }

    /**
     * Advances one tick.
     *
     * @param buried  the head is inside solid ground
     * @param falling the head is moving down
     * @return true on the tick a leap starts
     */
    public boolean tick(boolean hasTarget, double horizontalDistance, boolean buried, boolean falling) {
        switch (phase) {
            case ROAM:
                if (cooldownLeft > 0) cooldownLeft--;
                boolean ready = hasTarget && horizontalDistance <= strikeRange && cooldownLeft <= 0;
                if (ready && (medium == Medium.AIR || buried)) {
                    enter(Phase.WARN);
                    if (warnTicks <= 0) return leap();
                }
                return false;
            case WARN:
                if (!hasTarget) {
                    enter(Phase.ROAM);
                    return false;
                }
                return ++elapsed >= warnTicks && leap();
            default:
                elapsed++;
                boolean landed = medium == Medium.GROUND ? falling && buried : elapsed >= leapTicks;
                if (landed || elapsed >= maxLeapTicks) {
                    enter(Phase.ROAM);
                    cooldownLeft = cooldown;
                }
                return false;
        }
    }

    public Phase phase() {
        return phase;
    }

    /** Ticks spent in the current phase. */
    public int elapsed() {
        return elapsed;
    }

    /** How many dust puffs a warning shows on the given tick: one at first, thickening to four. */
    public static int warnDust(int elapsed, int warnTicks) {
        int span = Math.max(1, warnTicks - 1);
        return Math.min(MAX_DUST, 1 + (MAX_DUST - 1) * Math.max(0, elapsed) / span);
    }

    private boolean leap() {
        enter(Phase.LEAP);
        return true;
    }

    private void enter(Phase next) {
        phase = next;
        elapsed = 0;
    }
}

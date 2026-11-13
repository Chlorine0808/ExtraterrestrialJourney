package chlorine.etjourney.content.rank;

/** How a summoned minion follows its parent, checked once a second. */
public final class MinionWatch {

    public enum Parent {
        ALIVE,
        DEAD,
        MISSING
    }

    public static final int VANISH = -1;
    /** Checks a parent may stay missing, so a reloaded minion outlives a parent read a moment later. */
    public static final int GRACE = 3;

    private MinionWatch() {}

    /** The misses after this check, or VANISH. */
    public static int step(int misses, Parent parent) {
        switch (parent) {
            case ALIVE:
                return 0;
            case DEAD:
                return VANISH;
            default:
                return misses + 1 >= GRACE ? VANISH : misses + 1;
        }
    }

    public static int toSummon(int alive, int cap, int perCast) {
        return Math.max(0, Math.min(perCast, cap - alive));
    }
}

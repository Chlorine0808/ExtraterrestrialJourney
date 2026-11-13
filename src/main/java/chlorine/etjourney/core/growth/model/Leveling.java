package chlorine.etjourney.core.growth.model;

/** Experience and levels: level L to L + 1 takes 10 (L + 1)^2. */
public final class Leveling {

    public static final int MAX_LEVEL = 10;

    private Leveling() {}

    public static int toNext(int level) {
        return 10 * (level + 1) * (level + 1);
    }

    /** Experience needed from 0 to reach the level. */
    public static int totalFor(int level) {
        int sum = 0;
        for (int l = 0; l < level; l++) sum += toNext(l);
        return sum;
    }

    public static int level(int xp) {
        int level = 0;
        while (level < MAX_LEVEL && xp >= totalFor(level + 1)) level++;
        return level;
    }

    /** Experience into the current level and the amount the level takes; both 0 at the maximum. */
    public static int[] progress(int xp) {
        int level = level(xp);
        if (level >= MAX_LEVEL) return new int[] { 0, 0 };
        return new int[] { xp - totalFor(level), toNext(level) };
    }
}

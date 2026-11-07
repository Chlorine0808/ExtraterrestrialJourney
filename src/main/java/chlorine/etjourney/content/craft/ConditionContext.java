package chlorine.etjourney.content.craft;

/** What a device knows about where it stands, read once from the world so conditions never touch it. */
public final class ConditionContext {

    private static final long DAY = 24000L;
    /** Vanilla lets monsters spawn in the open from about here until dawn. */
    private static final long DUSK = 13000L, DAWN = 23000L;

    final String biome;
    final int dimension;
    final boolean canSeeSky;
    /** Whether the sky here turns from day to night; the End and the Nether keep a clock but show nothing of it. */
    final boolean hasDayCycle;
    final long worldTime;

    public ConditionContext(String biome, int dimension, boolean canSeeSky, boolean hasDayCycle, long worldTime) {
        this.biome = biome;
        this.dimension = dimension;
        this.canSeeSky = canSeeSky;
        this.hasDayCycle = hasDayCycle;
        this.worldTime = worldTime;
    }

    /** Never where the sky has no day and night. */
    boolean isNight() {
        if (!hasDayCycle) return false;
        long time = Math.floorMod(worldTime, DAY);
        return time >= DUSK && time < DAWN;
    }
}

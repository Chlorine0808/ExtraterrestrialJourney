package chlorine.etjourney.content.rank;

/** Waits between uses of an ability, counted in total world ticks. */
public final class Cooldown {

    public static final long NEVER = -1;

    private Cooldown() {}

    public static boolean ready(long now, long last, int length) {
        return last == NEVER || now < last || now - last >= length;
    }
}

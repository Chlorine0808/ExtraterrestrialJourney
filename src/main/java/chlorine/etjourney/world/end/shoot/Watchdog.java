package chlorine.etjourney.world.end.shoot;

/** Fires when a run has made no progress for longer than its limit; times are in milliseconds. */
public final class Watchdog {

    private final long limit;
    private long last;

    public Watchdog(long limit, long now) {
        this.limit = limit;
        this.last = now;
    }

    public void progress(long now) {
        last = now;
    }

    public boolean expired(long now) {
        return now - last > limit;
    }
}

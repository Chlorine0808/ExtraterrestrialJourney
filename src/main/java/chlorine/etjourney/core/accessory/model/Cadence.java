package chlorine.etjourney.core.accessory.model;

/** Runs something once per period of world ticks, however often it is asked within one tick. */
public final class Cadence {

    private final long period;
    private long last;
    private boolean started;

    public Cadence(long period) {
        this.period = period;
    }

    /** A clock that went back, as in another world, counts as due. */
    public boolean due(long now) {
        if (started && now >= last && now - last < period) return false;
        started = true;
        last = now;
        return true;
    }
}

package chlorine.etjourney.core.combat.model;

/** Where a gate stands: charges spent, since when they recharge, when the cooldown ends and the last firing. */
public final class GateState {

    public static final GateState FRESH = new GateState(0, 0, 0, Long.MIN_VALUE);

    public final int spent;
    public final long since;
    public final long readyAt;
    public final long lastFired;

    public GateState(int spent, long since, long readyAt, long lastFired) {
        this.spent = spent;
        this.since = since;
        this.readyAt = readyAt;
        this.lastFired = lastFired;
    }
}

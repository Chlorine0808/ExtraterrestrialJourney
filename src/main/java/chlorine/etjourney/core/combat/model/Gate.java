package chlorine.etjourney.core.combat.model;

/**
 * How often a hook may fire: up to a number of charges, each coming back after a recharge time, with a cooldown
 * between uses. A recharge of 0 means spent charges never come back.
 */
public final class Gate {

    public final int charges;
    public final long cooldown;
    public final long recharge;

    public Gate(int charges, long cooldown, long recharge) {
        if (charges < 1) throw new IllegalArgumentException("charges must be >= 1: " + charges);
        this.charges = charges;
        this.cooldown = Math.max(0, cooldown);
        this.recharge = Math.max(0, recharge);
    }

    /** A gate that fires once per period of ticks. */
    public static Gate every(long ticks) {
        return new Gate(1, 0, ticks);
    }

    /** A gate of a few uses that never come back. */
    public static Gate uses(int charges) {
        return new Gate(charges, 0, 0);
    }

    public boolean ready(GateState state, long now) {
        GateState s = settle(state, now);
        return s.spent < charges && now >= s.readyAt;
    }

    /** The state after firing now; unchanged but settled when the gate is not ready. */
    public GateState fire(GateState state, long now) {
        GateState s = settle(state, now);
        if (s.spent >= charges || now < s.readyAt) return s;
        long since = s.spent == 0 ? now : s.since;
        return new GateState(s.spent + 1, since, now + cooldown, now);
    }

    /** Charges left now. */
    public int left(GateState state, long now) {
        return charges - settle(state, now).spent;
    }

    /** Whether every charge is used for good. */
    public boolean exhausted(GateState state, long now) {
        return recharge == 0 && settle(state, now).spent >= charges;
    }

    /** Gives back the charges that recharged by now; a clock that went back forgets everything. */
    GateState settle(GateState s, long now) {
        if (now < s.lastFired) return GateState.FRESH;
        if (recharge == 0 || s.spent == 0) return s;
        long back = (now - s.since) / recharge;
        if (back <= 0) return s;
        int spent = (int) Math.max(0, s.spent - back);
        long since = spent == 0 ? now : s.since + back * recharge;
        return new GateState(spent, since, s.readyAt, s.lastFired);
    }
}

package chlorine.etjourney.core.status.model;

/** The duration and level a status effect lands with, after the giver's boosts and the taker's resistance. */
public final class StatusMath {

    /** The effect name that stands for every harmful effect. */
    public static final String ANY = "*";

    /** Duration in ticks and vanilla amplifier (0 is level I). */
    public static final class Result {

        public final int duration;
        public final int amplifier;

        Result(int duration, int amplifier) {
            this.duration = duration;
            this.amplifier = amplifier;
        }
    }

    private StatusMath() {}

    /** Null when nothing is left to apply. Cuts never take the level below I. */
    public static Result resolve(int duration, int amplifier, String effect, boolean harmful, StatusBoosts boosts,
        StatusResistance resistance) {
        int ticks = Math
            .round(duration * boosts.durationFactor(effect, harmful) * resistance.durationFactor(effect, harmful));
        if (ticks <= 0) return null;
        int level = amplifier + boosts.levelBonus(effect, harmful) - resistance.levelCut(effect, harmful);
        return new Result(ticks, Math.max(0, level));
    }
}

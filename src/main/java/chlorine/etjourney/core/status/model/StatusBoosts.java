package chlorine.etjourney.core.status.model;

import java.util.HashMap;
import java.util.Map;

/** A giver's duration bonus and extra levels per effect name; both add up. */
public final class StatusBoosts {

    public static final StatusBoosts NONE = builder().build();

    private final Map<String, Float> bonuses;
    private final Map<String, Integer> levels;

    private StatusBoosts(Builder b) {
        this.bonuses = new HashMap<>(b.bonuses);
        this.levels = new HashMap<>(b.levels);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** {@link StatusMath#ANY} counts only for a harmful effect; the factor stops at 0. */
    public float durationFactor(String effect, boolean harmful) {
        float sum = 1 + bonuses.getOrDefault(effect, 0f);
        if (harmful) sum += bonuses.getOrDefault(StatusMath.ANY, 0f);
        return Math.max(0, sum);
    }

    public int levelBonus(String effect, boolean harmful) {
        int own = levels.getOrDefault(effect, 0);
        return harmful ? own + levels.getOrDefault(StatusMath.ANY, 0) : own;
    }

    public static final class Builder {

        private final Map<String, Float> bonuses = new HashMap<>();
        private final Map<String, Integer> levels = new HashMap<>();

        private Builder() {}

        /** A bonus of 0.5 makes the effect last 50% longer. */
        public Builder addDuration(String effect, float bonus) {
            bonuses.merge(effect, bonus, Float::sum);
            return this;
        }

        public Builder addLevel(String effect, int extra) {
            levels.merge(effect, extra, Integer::sum);
            return this;
        }

        public StatusBoosts build() {
            return new StatusBoosts(this);
        }
    }
}

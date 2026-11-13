package chlorine.etjourney.core.status.model;

import java.util.HashMap;
import java.util.Map;

/** A taker's duration factor and level cut per effect name; factors multiply, cuts add. */
public final class StatusResistance {

    public static final StatusResistance NONE = builder().build();

    private final Map<String, Float> factors;
    private final Map<String, Integer> cuts;

    private StatusResistance(Builder b) {
        this.factors = new HashMap<>(b.factors);
        this.cuts = new HashMap<>(b.cuts);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** {@link StatusMath#ANY} counts only for a harmful effect. */
    public float durationFactor(String effect, boolean harmful) {
        float own = factors.getOrDefault(effect, 1f);
        return harmful ? own * factors.getOrDefault(StatusMath.ANY, 1f) : own;
    }

    public int levelCut(String effect, boolean harmful) {
        int own = cuts.getOrDefault(effect, 0);
        return harmful ? own + cuts.getOrDefault(StatusMath.ANY, 0) : own;
    }

    public static final class Builder {

        private final Map<String, Float> factors = new HashMap<>();
        private final Map<String, Integer> cuts = new HashMap<>();

        private Builder() {}

        /** A factor of 0.5 halves the duration; 0 keeps the effect off. */
        public Builder multiplyDuration(String effect, float factor) {
            factors.merge(effect, factor, (a, b) -> a * b);
            return this;
        }

        public Builder reduceLevel(String effect, int levels) {
            cuts.merge(effect, levels, Integer::sum);
            return this;
        }

        public StatusResistance build() {
            return new StatusResistance(this);
        }
    }
}

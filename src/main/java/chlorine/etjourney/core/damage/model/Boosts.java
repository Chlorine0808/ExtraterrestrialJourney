package chlorine.etjourney.core.damage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An attacker's damage bonuses; every matching bonus adds to a factor of 1, which stops at 0. */
public final class Boosts {

    public static final Boosts NONE = builder().build();

    private final List<BoostFilter> filters;
    private final List<Float> bonuses;

    private Boosts(Builder b) {
        this.filters = Collections.unmodifiableList(new ArrayList<>(b.filters));
        this.bonuses = Collections.unmodifiableList(new ArrayList<>(b.bonuses));
    }

    public static Builder builder() {
        return new Builder();
    }

    public float factor(Element element, AttackProfile attack) {
        float sum = 1;
        for (int i = 0; i < filters.size(); i++) if (filters.get(i)
            .matches(element, attack)) sum += bonuses.get(i);
        return Math.max(0, sum);
    }

    public static final class Builder {

        private final List<BoostFilter> filters = new ArrayList<>();
        private final List<Float> bonuses = new ArrayList<>();

        private Builder() {}

        /** A bonus of 0.3 adds 30% to every matching part. */
        public Builder add(BoostFilter filter, float bonus) {
            filters.add(filter);
            bonuses.add(bonus);
            return this;
        }

        public Builder addAll(Boosts other) {
            filters.addAll(other.filters);
            bonuses.addAll(other.bonuses);
            return this;
        }

        public Boosts build() {
            return new Boosts(this);
        }
    }
}

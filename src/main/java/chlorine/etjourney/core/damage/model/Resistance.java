package chlorine.etjourney.core.damage.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** A defender's factor and flat reduction per element, and factor per kind; factors multiply, flats add. */
public final class Resistance {

    public static final Resistance NONE = builder().build();

    private final Map<Element, Float> factors;
    private final Map<Element, Float> flats;
    private final Map<DamageKind, Float> kindFactors;

    private Resistance(Builder b) {
        this.factors = new HashMap<>(b.factors);
        this.flats = new HashMap<>(b.flats);
        this.kindFactors = new EnumMap<>(b.kindFactors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public float factor(Element element) {
        return factors.getOrDefault(element, 1f);
    }

    public float flat(Element element) {
        return flats.getOrDefault(element, 0f);
    }

    public float kindFactor(DamageKind kind) {
        return kindFactors.getOrDefault(kind, 1f);
    }

    public static final class Builder {

        private final Map<Element, Float> factors = new HashMap<>();
        private final Map<Element, Float> flats = new HashMap<>();
        private final Map<DamageKind, Float> kindFactors = new EnumMap<>(DamageKind.class);

        private Builder() {}

        public Builder multiply(Element element, float factor) {
            factors.merge(element, factor, (a, b) -> a * b);
            return this;
        }

        public Builder subtract(Element element, float amount) {
            flats.merge(element, amount, Float::sum);
            return this;
        }

        public Builder multiplyKind(DamageKind kind, float factor) {
            kindFactors.merge(kind, factor, (a, b) -> a * b);
            return this;
        }

        public Resistance build() {
            return new Resistance(this);
        }
    }
}

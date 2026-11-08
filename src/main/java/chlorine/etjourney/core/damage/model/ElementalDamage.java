package chlorine.etjourney.core.damage.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One attack: an amount per element in the order added, and its kind. */
public final class ElementalDamage {

    private final Map<Element, Float> parts;
    private final DamageKind kind;

    private ElementalDamage(Map<Element, Float> parts, DamageKind kind) {
        this.parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        this.kind = kind;
    }

    public static Builder builder(DamageKind kind) {
        return new Builder(kind);
    }

    public Map<Element, Float> parts() {
        return parts;
    }

    public DamageKind kind() {
        return kind;
    }

    /** Whether every part is absolute; a mixed attack pierces only through {@link #absolutePart()}. */
    public boolean isPiercing() {
        if (parts.isEmpty()) return false;
        for (Element element : parts.keySet()) if (!element.isAbsolute()) return false;
        return true;
    }

    /** The absolute parts alone, of the same kind. */
    public ElementalDamage absolutePart() {
        return only(true);
    }

    /** Every part but the absolute ones, of the same kind. */
    public ElementalDamage ordinaryPart() {
        return only(false);
    }

    private ElementalDamage only(boolean absolute) {
        Map<Element, Float> out = new LinkedHashMap<>();
        for (Map.Entry<Element, Float> e : parts.entrySet()) if (e.getKey()
            .isAbsolute() == absolute) out.put(e.getKey(), e.getValue());
        return new ElementalDamage(out, kind);
    }

    /** The sum of every part before any resistance. */
    public float raw() {
        float sum = 0;
        for (float amount : parts.values()) sum += amount;
        return sum;
    }

    /** The element with the largest amount, the first added on a tie, or neutral when there is none. */
    public Element dominant() {
        Element best = ElementRegistry.NEUTRAL;
        float most = -1;
        for (Map.Entry<Element, Float> e : parts.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public static final class Builder {

        private final Map<Element, Float> parts = new LinkedHashMap<>();
        private final DamageKind kind;

        private Builder(DamageKind kind) {
            if (kind == null) throw new IllegalArgumentException("kind is null");
            this.kind = kind;
        }

        /** Adds to the amount of that element. */
        public Builder add(Element element, float amount) {
            if (element == null) throw new IllegalArgumentException("element is null");
            if (!(amount >= 0)) throw new IllegalArgumentException("amount must be >= 0: " + amount);
            parts.merge(element, amount, Float::sum);
            return this;
        }

        public ElementalDamage build() {
            return new ElementalDamage(parts, kind);
        }
    }
}

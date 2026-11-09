package chlorine.etjourney.core.damage.model;

import java.util.Map;

/** The damage formula of the elemental damage spec, section 1. */
public final class DamageMath {

    private DamageMath() {}

    /**
     * Sum over elements of max(0, amount x element factor x kind factor - flat); a negative factor counts as 0,
     * and absolute elements skip the kind factor.
     */
    public static float resolve(ElementalDamage damage, Resistance resistance) {
        float kind = Math.max(0, resistance.kindFactor(damage.kind()));
        float total = 0;
        for (Map.Entry<Element, Float> e : damage.parts()
            .entrySet()) {
            Element element = e.getKey();
            float factor = Math.max(0, resistance.factor(element)) * (element.isAbsolute() ? 1 : kind);
            total += Math.max(0, e.getValue() * factor - resistance.flat(element));
        }
        return total;
    }

    /**
     * Scales an amount that reached the defender by resolved / raw, so a part-hit inside the hurt cooldown or an
     * amount another mod already changed keeps its share; unchanged when the raw sum is 0.
     */
    public static float scale(float incoming, ElementalDamage damage, Resistance resistance) {
        float raw = damage.raw();
        if (raw <= 0) return incoming;
        return incoming * resolve(damage, resistance) / raw;
    }

    /** How much of the expected loss the hit failed to take, from health plus absorption before and after. */
    public static float shortfall(float before, float after, float expected) {
        return Math.max(0, expected - (before - after));
    }
}

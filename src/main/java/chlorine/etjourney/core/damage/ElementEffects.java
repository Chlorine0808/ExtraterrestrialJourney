package chlorine.etjourney.core.damage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.EntityLivingBase;

import chlorine.etjourney.core.damage.model.Element;

/** What happens on a hit per element, such as freezing for ice; nothing is registered yet. */
public final class ElementEffects {

    /** Runs once per hit for each element of the attack with an amount above 0. */
    public interface Effect {

        void onHit(EntityLivingBase target, ElementalDamageSource source, float amount);
    }

    private static final Map<Element, Effect> EFFECTS = new ConcurrentHashMap<>();

    private ElementEffects() {}

    public static void register(Element element, Effect effect) {
        if (EFFECTS.putIfAbsent(element, effect) != null)
            throw new IllegalArgumentException("Effect already registered for " + element);
    }

    static void fire(EntityLivingBase target, ElementalDamageSource source) {
        for (Map.Entry<Element, Float> part : source.damage()
            .parts()
            .entrySet()) {
            Effect effect = EFFECTS.get(part.getKey());
            if (effect != null && part.getValue() > 0) effect.onHit(target, source, part.getValue());
        }
    }
}

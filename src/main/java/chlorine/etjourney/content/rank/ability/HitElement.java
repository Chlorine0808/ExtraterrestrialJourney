package chlorine.etjourney.content.rank.ability;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.combat.Elements;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.core.damage.ElementalDamageSource;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;

/** The element of a hit, and the element an ability such as absorb is set to. */
final class HitElement {

    /** Neutral and chaos are left out: warding neutral is ethereal, and chaos cannot be warded. */
    private static final String[] CHOICES = { "fire", "ice", "wind", "electric", "order", "light" };

    private HitElement() {}

    /** The strongest element of an elemental hit, fire for vanilla fire, otherwise neutral. */
    static Element of(DamageSource source) {
        if (source instanceof ElementalDamageSource) return ((ElementalDamageSource) source).damage()
            .dominant();
        return source.isFireDamage() ? Elements.FIRE : ElementRegistry.NEUTRAL;
    }

    static List<Element> choices() {
        Elements.register();
        List<Element> out = new ArrayList<>();
        for (String name : CHOICES) {
            Element e = ElementRegistry.get(name);
            if (e != null) out.add(e);
        }
        return out;
    }

    /** Picks an element at random unless the ability was already set to one. */
    static void chooseIfUnset(EntityLiving self, String ability) {
        if (!RankData.setting(self, ability)
            .isEmpty()) return;
        List<Element> all = choices();
        RankData.setSetting(
            self,
            ability,
            all.get(
                self.getRNG()
                    .nextInt(all.size()))
                .name());
    }

    /** Whether the hit is of the element the ability is set to. */
    static boolean matches(EntityLiving self, String ability, DamageSource source) {
        return of(source).name()
            .equals(RankData.setting(self, ability));
    }
}

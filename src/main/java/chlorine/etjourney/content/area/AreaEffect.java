package chlorine.etjourney.content.area;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

/** What an area does to a creature each time it works on it, beyond its damage. */
public interface AreaEffect {

    /** After the damage, if any; hurt is whether health or absorption dropped. */
    void onPulse(EntityLingeringArea area, EntityLivingBase target, boolean hurt);

    /** Gives a potion effect, even when the damage bounced off. */
    static AreaEffect status(int potionId, int ticks, int amplifier) {
        return (area, target, hurt) -> AreaStatus
            .apply(target, new PotionEffect(potionId, ticks, amplifier), area.owner(), false);
    }

    /** Sets the creature alight. */
    static AreaEffect burn(int seconds) {
        return (area, target, hurt) -> target.setFire(seconds);
    }
}

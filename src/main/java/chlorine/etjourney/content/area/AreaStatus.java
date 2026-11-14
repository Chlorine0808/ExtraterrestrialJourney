package chlorine.etjourney.content.area;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.core.status.StatusEffects;

/** The one way areas give status effects, so resistances, boosts and milk-proof effects apply to them. */
public final class AreaStatus {

    private AreaStatus() {}

    /** Gives the effect through StatusEffects.apply; false when nothing landed. */
    public static boolean apply(EntityLivingBase target, PotionEffect effect, EntityLivingBase giver,
        boolean stubborn) {
        return StatusEffects.apply(target, effect, giver, stubborn);
    }
}

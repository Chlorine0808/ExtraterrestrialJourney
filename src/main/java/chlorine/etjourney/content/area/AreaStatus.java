package chlorine.etjourney.content.area;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

/**
 * The one way areas give status effects. A stand-in until the status effect base lands; it then forwards to
 * StatusEffects.apply, which takes the same arguments, so resistances and milk-proof effects apply.
 */
public final class AreaStatus {

    private AreaStatus() {}

    /** Gives the effect; giver and stubborn are kept for StatusEffects.apply and ignored for now. */
    public static boolean apply(EntityLivingBase target, PotionEffect effect, EntityLivingBase giver,
        boolean stubborn) {
        if (target.worldObj.isRemote || !target.isPotionApplicable(effect)) return false;
        target.addPotionEffect(effect);
        return true;
    }
}

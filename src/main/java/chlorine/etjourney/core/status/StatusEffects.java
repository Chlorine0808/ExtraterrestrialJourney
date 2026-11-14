package chlorine.etjourney.core.status;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.core.status.model.StatusMath;

/** The way in for status effects: the giver's boosts and the taker's resistance apply, and milk can be kept out. */
public final class StatusEffects {

    private StatusEffects() {}

    /**
     * Gives the effect, scaled by the giver (null for none) and the target. A stubborn effect does not wash off with
     * milk. False when nothing landed: on the client, when resistance took it all, or when the target refuses it.
     */
    public static boolean apply(EntityLivingBase target, PotionEffect effect, EntityLivingBase giver,
        boolean stubborn) {
        if (target.worldObj.isRemote) return false;
        Potion potion = Potion.potionTypes[effect.getPotionID()];
        if (potion == null) return false;
        StatusMath.Result r = StatusMath.resolve(
            effect.getDuration(),
            effect.getAmplifier(),
            potion.getName(),
            Potions.harmful(potion),
            StatusSources.boostsOf(giver),
            StatusSources.resistanceOf(target));
        if (r == null) return false;
        PotionEffect landed = new PotionEffect(potion.id, r.duration, r.amplifier, effect.getIsAmbient());
        if (!target.isPotionApplicable(landed)) return false;
        target.addPotionEffect(landed);
        if (stubborn) makeStubborn(target, potion);
        return true;
    }

    /** Makes an effect the target already has milk-proof; false if it has none. */
    public static boolean makeStubborn(EntityLivingBase target, Potion potion) {
        PotionEffect active = target.getActivePotionEffect(potion);
        if (active == null) return false;
        StubbornEffects.makeStubborn(target, active);
        return true;
    }

    public static boolean isStubborn(EntityLivingBase target, Potion potion) {
        PotionEffect active = target.getActivePotionEffect(potion);
        return active != null && active.getCurativeItems()
            .isEmpty();
    }
}

package chlorine.etjourney.core.status;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.core.damage.ElementEffects;
import chlorine.etjourney.core.damage.ElementalDamageSource;

/** An element effect that gives a status effect on a hit, with the attacker as the giver. */
public final class StatusOnHit implements ElementEffects.Effect {

    private final Potion potion;
    private final int ticks;
    private final int amplifier;
    private final boolean stubborn;

    public StatusOnHit(Potion potion, int ticks, int amplifier, boolean stubborn) {
        this.potion = potion;
        this.ticks = ticks;
        this.amplifier = amplifier;
        this.stubborn = stubborn;
    }

    @Override
    public void onHit(EntityLivingBase target, ElementalDamageSource source, float amount) {
        Entity attacker = source.getEntity();
        StatusEffects.apply(
            target,
            new PotionEffect(potion.id, ticks, amplifier),
            attacker instanceof EntityLivingBase ? (EntityLivingBase) attacker : null,
            stubborn);
    }
}

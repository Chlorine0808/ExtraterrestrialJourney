package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import chlorine.etjourney.core.damage.AttackProfiles;
import chlorine.etjourney.core.damage.model.AttackProfile;

/**
 * One hit as the hooks see it; for onHurt the amount is before vanilla armor and may be changed, for onDamageDealt
 * it is the health the hit took.
 */
public final class CombatHit {

    public final EntityLivingBase target;
    /** Null when no living creature is to blame. */
    public final EntityLivingBase attacker;
    public final DamageSource source;
    public final AttackProfile attack;
    private float amount;

    public CombatHit(EntityLivingBase target, EntityLivingBase attacker, DamageSource source, AttackProfile attack,
        float amount) {
        this.target = target;
        this.attacker = attacker;
        this.source = source;
        this.attack = attack;
        this.amount = amount;
    }

    public static CombatHit of(LivingHurtEvent event) {
        EntityLivingBase attacker = event.source.getEntity() instanceof EntityLivingBase
            ? (EntityLivingBase) event.source.getEntity()
            : null;
        return new CombatHit(
            event.entityLiving,
            attacker,
            event.source,
            AttackProfiles.of(event.source),
            event.ammount);
    }

    public float amount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = Math.max(0, amount);
    }
}

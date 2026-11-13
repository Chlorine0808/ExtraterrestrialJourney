package chlorine.etjourney.core.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.core.damage.model.AttackProfile;

/** One hit as the hooks see it; the amount is before vanilla armor, and onHurt hooks may change it. */
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

    public float amount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = Math.max(0, amount);
    }
}

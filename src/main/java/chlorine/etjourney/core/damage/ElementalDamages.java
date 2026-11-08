package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.core.damage.model.DamageMath;
import chlorine.etjourney.core.damage.model.ElementalDamage;
import chlorine.etjourney.core.util.ModLog;

/** The way in for elemental attacks. */
public final class ElementalDamages {

    /** DataWatcher slot of EntityLivingBase health. */
    private static final int HEALTH_WATCHER = 6;

    private ElementalDamages() {}

    /**
     * Hits the target and runs the element effects when health or absorption dropped. Only the absolute parts
     * pierce, so a mixed attack lands as two hits: the ordinary part first, under the hurt cooldown, then the
     * absolute part.
     */
    public static boolean deal(EntityLivingBase target, ElementalDamageSource source) {
        // Damage is the server's; the fallback would otherwise kill the client's copy of a player.
        if (target.worldObj.isRemote) return false;
        // PvP off or same team: stop here, or the piercing fallback would hit anyway.
        if (target instanceof EntityPlayer && source.getEntity() instanceof EntityPlayer
            && !((EntityPlayer) target).canAttackPlayer((EntityPlayer) source.getEntity())) return false;
        ElementalDamage damage = source.damage();
        float before = life(target);
        ElementalDamage absolute = damage.absolutePart();
        if (damage.isPiercing() || absolute.parts()
            .isEmpty()) hitOnce(target, source);
        else {
            hitOnce(target, part(source, damage.ordinaryPart()));
            hitOnce(target, part(source, absolute));
        }
        boolean hit = life(target) < before;
        if (hit) ElementEffects.fire(target, source);
        return hit;
    }

    private static ElementalDamageSource part(ElementalDamageSource source, ElementalDamage damage) {
        return new ElementalDamageSource(damage, source.getSourceOfDamage(), source.getEntity());
    }

    /**
     * One hit of a single source. Piercing damage ignores the hurt cooldown, and on a player takes directly whatever
     * armor outside the events held back.
     */
    private static void hitOnce(EntityLivingBase target, ElementalDamageSource source) {
        ElementalDamage damage = source.damage();
        boolean pierce = damage.isPiercing() && pierces(target);
        if (pierce) target.hurtResistantTime = 0;
        float before = life(target);
        target.attackEntityFrom(source, damage.raw());
        if (pierce && target instanceof EntityPlayer && !target.isDead && target.getHealth() > 0) {
            float expected = DamageMath.resolve(damage, ResistanceSources.of(target));
            float missing = DamageMath.shortfall(before, life(target), expected);
            if (missing > 0) {
                ModLog.LOG.debug("piercing fallback takes {} from {}", missing, target.getCommandSenderName());
                takeDirectly(target, source, missing);
            }
        }
    }

    /** Whether piercing damage may get past the target's immunities: never creative players or invulnerables. */
    public static boolean pierces(EntityLivingBase target) {
        if (target.isEntityInvulnerable()) return false;
        return !(target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage);
    }

    private static float life(EntityLivingBase target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    /** Takes from absorption first, then health, past any setHealth override; kills at 0. */
    private static void takeDirectly(EntityLivingBase target, ElementalDamageSource source, float amount) {
        float absorption = target.getAbsorptionAmount();
        float fromAbsorption = Math.min(absorption, amount);
        target.setAbsorptionAmount(absorption - fromAbsorption);
        float health = Math.max(0, target.getHealth() - (amount - fromAbsorption));
        // The death message comes from the combat tracker, so record this hit as vanilla damage would.
        target.func_110142_aN()
            .func_94547_a(source, target.getHealth(), amount - fromAbsorption);
        target.setHealth(health);
        if (target.getHealth() > health) target.getDataWatcher()
            .updateObject(HEALTH_WATCHER, health);
        if (health <= 0) target.onDeath(source);
    }
}

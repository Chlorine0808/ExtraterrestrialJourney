package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.potion.Potion;

import chlorine.etjourney.content.rank.ability.EnrageAbility;

/** enrage: no strength while healthy, strength once health is below 40%. */
final class EnrageTrial extends AbilityTrial {

    private EntityLiving mob;

    EnrageTrial() {
        super(EnrageAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, EnrageAbility.ID);
            return false;
        }
        if (tick == 25) {
            c.verdict(!mob.isPotionActive(Potion.damageBoost), name + ": no strength at full health");
            mob.setHealth(mob.getMaxHealth() * 0.3f);
            return false;
        }
        if (tick < 50) return false;
        c.verdict(
            mob.isPotionActive(Potion.damageBoost) && mob.isPotionActive(Potion.moveSpeed),
            name + ": strength and speed below 40% health");
        return true;
    }
}

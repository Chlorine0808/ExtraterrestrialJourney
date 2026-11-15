package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.potion.Potion;

import chlorine.etjourney.content.rank.ability.BuffsAbility;

/** buffs: the three potion effects are on the mob after a second or so. */
final class BuffsTrial extends AbilityTrial {

    private EntityLiving mob;

    BuffsTrial() {
        super(BuffsAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, BuffsAbility.ID);
            return false;
        }
        if (tick < 25) return false;
        c.verdict(
            mob.isPotionActive(Potion.damageBoost) && mob.isPotionActive(Potion.resistance)
                && mob.isPotionActive(Potion.fireResistance),
            name + ": strength, resistance and fire resistance are on");
        return true;
    }
}

package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.ShieldAbility;

/** shield: absorption at spawn, cleared by a big hit, back after about nine seconds. */
final class ShieldTrial extends AbilityTrial {

    private EntityLiving mob;

    ShieldTrial() {
        super(ShieldAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, ShieldAbility.ID);
            return false;
        }
        if (tick == 5) {
            c.verdict(mob.getAbsorptionAmount() > 0, name + ": absorption at spawn");
            mob.attackEntityFrom(c.byPlayer(), mob.getAbsorptionAmount() + 1);
            return false;
        }
        if (tick == 10) {
            c.verdict(mob.getAbsorptionAmount() <= 0, name + ": a hit clears the absorption");
            return false;
        }
        if (tick < 200) return false;
        c.verdict(mob.getAbsorptionAmount() > 0, name + ": absorption is back");
        return true;
    }
}

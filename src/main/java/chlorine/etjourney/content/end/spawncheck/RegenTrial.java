package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.RegenAbility;

/** regen: a mob hurt by some health heals back within a couple of seconds. */
final class RegenTrial extends AbilityTrial {

    private EntityLiving mob;
    private float hurt;

    RegenTrial() {
        super(RegenAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, RegenAbility.ID);
            return false;
        }
        if (tick == 2) {
            mob.setHealth(mob.getMaxHealth() / 2);
            hurt = mob.getHealth();
            return false;
        }
        if (tick < 50) return false;
        c.verdict(mob.getHealth() > hurt, name + ": health rose from " + hurt + " to " + mob.getHealth());
        return true;
    }
}

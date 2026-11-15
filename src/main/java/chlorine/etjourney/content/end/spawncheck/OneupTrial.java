package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.OneupAbility;

/** oneup: the first huge hit leaves the mob at full health, the second kills it. */
final class OneupTrial extends AbilityTrial {

    private EntityLiving mob;

    OneupTrial() {
        super(OneupAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, OneupAbility.ID);
            mob.attackEntityFrom(c.byPlayer(), 1000);
            return false;
        }
        if (tick == 30) {
            c.verdict(
                mob.isEntityAlive() && mob.getHealth() >= mob.getMaxHealth() - 0.01,
                name + ": alive at full health after the first blow");
            mob.attackEntityFrom(c.byPlayer(), 1000);
            return false;
        }
        if (tick < 35) return false;
        c.verdict(!mob.isEntityAlive(), name + ": dead after the second blow");
        return true;
    }
}

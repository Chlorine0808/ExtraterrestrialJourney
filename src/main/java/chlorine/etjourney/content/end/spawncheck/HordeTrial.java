package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.HordeAbility;
import chlorine.etjourney.content.rank.ability.SummonAbility;

/** horde: a hit makes minions appear, and they vanish when the parent dies. */
final class HordeTrial extends AbilityTrial {

    private EntityLiving mob;

    HordeTrial() {
        super(HordeAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(Rank.EPIC, 4, 0, HordeAbility.ID);
            mob.attackEntityFrom(c.byPlayer(), 1);
            return false;
        }
        if (tick == 45) {
            int minions = SummonAbility.minionsOf(mob)
                .size();
            c.verdict(minions >= 1, name + ": " + minions + " minions after a hit");
            mob.attackEntityFrom(c.byPlayer(), 1000);
            return false;
        }
        if (tick < 70) return false;
        int left = SummonAbility.minionsOf(mob)
            .size();
        c.verdict(left == 0, name + ": " + left + " minions left after the parent died");
        return true;
    }
}

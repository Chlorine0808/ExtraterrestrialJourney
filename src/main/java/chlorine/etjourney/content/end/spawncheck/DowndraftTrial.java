package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.DowndraftAbility;
import chlorine.etjourney.world.wind.WindWorld;

/** downdraft: a wind appears after the warning, and goes again 60 ticks later. */
final class DowndraftTrial extends AbilityTrial {

    private int base;
    private boolean rose;

    DowndraftTrial() {
        super(DowndraftAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        int dimension = c.end.provider.dimensionId;
        if (tick == 1) {
            base = WindWorld.SERVER.in(dimension)
                .size();
            EntityLiving mob = c.spawn(Rank.EPIC, 5, 0, DowndraftAbility.ID);
            TrialSupport.harmless(mob);
            mob.setAttackTarget(c.player);
            return false;
        }
        int now = WindWorld.SERVER.in(dimension)
            .size();
        if (now > base) rose = true;
        if (rose && now <= base) {
            c.verdict(true, name + ": the wind came and went");
            return true;
        }
        if (tick < 140) return false;
        c.verdict(false, name + ": the wind " + (rose ? "did not go" : "never came"));
        return true;
    }
}

package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.LightningAbility;

/** lightning: the player stands where the warning was, and the strike a second later hurts. */
final class LightningTrial extends AbilityTrial {

    LightningTrial() {
        super(LightningAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(Rank.EPIC, 5, 0, LightningAbility.ID);
            TrialSupport.harmless(mob);
            mob.setAttackTarget(c.player);
            return false;
        }
        boolean hurt = c.player.getHealth() < c.player.getMaxHealth();
        if (!hurt && tick < 60) return false;
        c.verdict(hurt, name + ": the player lost health to the strike");
        return true;
    }
}

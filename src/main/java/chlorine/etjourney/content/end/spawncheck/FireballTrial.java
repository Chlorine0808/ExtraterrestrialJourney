package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.FireballAbility;
import chlorine.etjourney.content.rank.ability.RankShots;

/** fireball: a mob that sees its target fires a fireball within 5 seconds. */
final class FireballTrial extends AbilityTrial {

    private boolean fired;

    FireballTrial() {
        super(FireballAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(6, 0, FireballAbility.ID);
            TrialSupport.harmless(mob);
            mob.setAttackTarget(c.player);
            return false;
        }
        if (TrialSupport.shotExists(c.end, RankShots.FIREBALL.name) || c.player.isBurning()) fired = true;
        if (!fired && tick < 100) return false;
        c.verdict(fired, name + ": a fireball was fired or the player burns");
        return true;
    }
}

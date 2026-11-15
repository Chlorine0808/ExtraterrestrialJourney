package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.RankShots;
import chlorine.etjourney.content.rank.ability.RockfallAbility;

/** rockfall: after the warning, icicles are in the air above the player. */
final class RockfallTrial extends AbilityTrial {

    private boolean fell;

    RockfallTrial() {
        super(RockfallAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(8, 0, RockfallAbility.ID);
            TrialSupport.harmless(mob);
            mob.setAttackTarget(c.player);
            return false;
        }
        if (TrialSupport.shotExists(c.end, RankShots.ICICLE.name)) fell = true;
        if (!fell && tick < 100) return false;
        c.verdict(fell, name + ": icicles fell after the warning");
        return true;
    }
}

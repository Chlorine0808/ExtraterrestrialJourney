package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.RouseAbility;

/** rouse: a plain zombie with no target near a mob fighting the player ends up fighting the player. */
final class RouseTrial extends AbilityTrial {

    private EntityLiving sleeper;

    RouseTrial() {
        super(RouseAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            EntityLiving mob = c.spawn(4, 0, RouseAbility.ID);
            mob.setAttackTarget(c.player);
            sleeper = c.plain(6, 3);
            return false;
        }
        if (tick < 110) return false;
        c.verdict(sleeper.getAttackTarget() == c.player, name + ": the plain zombie targets the player");
        return true;
    }
}

package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.GatherAbility;

/** gather: a plain zombie 12 blocks from a fighting gather mob ends up closer to it. */
final class GatherTrial extends AbilityTrial {

    private EntityLiving mob, far;
    private double start;

    GatherTrial() {
        super(GatherAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            mob = c.spawn(4, 0, GatherAbility.ID);
            mob.setAttackTarget(c.player);
            far = c.plain(-8, 0);
            start = far.getDistanceToEntity(mob);
            return false;
        }
        if (tick < 140) return false;
        double now = far.getDistanceToEntity(mob);
        c.verdict(now < start - 2, name + ": the far zombie came from " + start + " to " + now);
        return true;
    }
}

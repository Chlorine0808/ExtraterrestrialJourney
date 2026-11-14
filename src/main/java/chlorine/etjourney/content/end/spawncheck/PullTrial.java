package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.PullAbility;

/** pull: a player 12 blocks from a fighting mob is pushed toward it; flight is turned off for the trial. */
final class PullTrial extends AbilityTrial {

    private EntityLiving mob;
    private double startX, startZ;
    private boolean flying, moved;

    PullTrial() {
        super(PullAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) {
            flying = c.player.capabilities.isFlying;
            c.player.capabilities.isFlying = false;
            c.player.sendPlayerAbilities();
            mob = c.spawn(12, 0, PullAbility.ID);
            mob.setAttackTarget(c.player);
            startX = c.player.posX;
            startZ = c.player.posZ;
            return false;
        }
        double toward = (c.player.posX - startX) * Math.signum(mob.posX - startX)
            + (c.player.posZ - startZ) * Math.signum(mob.posZ - startZ) * 0;
        if (toward > 0.3) moved = true;
        if (tick < 110) return false;
        c.player.capabilities.isFlying = flying;
        c.player.sendPlayerAbilities();
        c.verdict(moved, name + ": the player moved toward the mob");
        return true;
    }
}

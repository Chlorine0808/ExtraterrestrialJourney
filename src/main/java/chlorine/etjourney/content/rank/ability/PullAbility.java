package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 5 seconds, a target 4 to 16 blocks away that the mob can see is pulled toward it. */
public final class PullAbility implements RankAbility {

    public static final String ID = "pull";
    static final int COOLDOWN = 100;
    static final double MIN = 4, MAX = 16, MAX_SPEED = 1.2, LIFT = 0.35, PER_BLOCK = 0.1;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, MAX);
        if (player == null || !self.canEntityBeSeen(player)) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        double[] v = pull(self.posX - player.posX, self.posZ - player.posZ, self.getDistanceToEntity(player));
        if (v == null) return;
        player.motionX = v[0];
        player.motionY = Math.max(player.motionY, v[1]);
        player.motionZ = v[2];
        player.velocityChanged = true;
        RankData.markUsed(self, ID, now);
        self.worldObj.playSoundAtEntity(self, "mob.endermen.portal", 1.0F, 0.6F);
        Offspring.puff(self, "portal", 16);
    }

    /** The player's motion {x, y, z} for a mob at dx, dz away and distance apart; null outside 4 to 16 blocks. */
    static double[] pull(double dx, double dz, double distance) {
        double flat = Math.hypot(dx, dz);
        if (distance < MIN || distance > MAX || flat < 1e-6) return null;
        double speed = Math.min(MAX_SPEED, flat * PER_BLOCK);
        return new double[] { dx / flat * speed, LIFT, dz / flat * speed };
    }
}

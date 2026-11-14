package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.projectile.Projectiles;
import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 4 seconds, a target within 20 blocks that the mob can see is shot with a fireball. */
public final class FireballAbility implements RankAbility {

    public static final String ID = "fireball";
    static final int COOLDOWN = 80;
    static final double REACH = 20;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, REACH);
        if (player == null || !self.canEntityBeSeen(player)) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        Projectiles
            .shootAt(RankShots.FIREBALL, self.worldObj, self, self.posX, self.posY + self.height * 0.7, self.posZ, player);
        RankData.markUsed(self, ID, now);
        self.worldObj.playSoundAtEntity(self, "mob.ghast.fireball", 1.0F, 1.0F);
    }
}

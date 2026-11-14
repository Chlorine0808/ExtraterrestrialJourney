package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.projectile.Projectiles;
import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 6 seconds, a seen target within 12 blocks is shot with a web that sticks where it lands. */
public final class WebsAbility implements RankAbility {

    public static final String ID = "webs";
    static final int COOLDOWN = 120;
    static final double REACH = 12;

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
            .shootAt(RankShots.WEB_SHOT, self.worldObj, self, self.posX, self.posY + self.height * 0.7, self.posZ, player);
        RankData.markUsed(self, ID, now);
        self.worldObj.playSoundAtEntity(self, "mob.spider.say", 1.0F, 1.0F);
    }
}

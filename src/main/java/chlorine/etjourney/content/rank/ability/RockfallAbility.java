package chlorine.etjourney.content.rank.ability;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.projectile.Projectiles;
import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 8 seconds, the spot under a target within 24 blocks is warned, then three icicles fall on it. */
public final class RockfallAbility implements RankAbility {

    public static final String ID = "rockfall";
    static final int COOLDOWN = 160, WARNING = 20, COUNT = 3;
    static final double REACH = 24, HEIGHT = 10, SPREAD = 1;
    private static final double[] DOWN = { 0, -1, 0 };

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, REACH);
        if (player == null) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        RankData.markUsed(self, ID, now);
        double x = player.posX, y = player.posY, z = player.posZ;
        Warning.at(self.worldObj, "snowshovel", x, y + 0.1, z, WARNING, () -> {
            if (!self.isEntityAlive()) return;
            Random random = self.getRNG();
            for (int i = 0; i < COUNT; i++) Projectiles.shootAlong(
                RankShots.ICICLE,
                self.worldObj,
                self,
                x + (random.nextDouble() * 2 - 1) * SPREAD,
                y + HEIGHT,
                z + (random.nextDouble() * 2 - 1) * SPREAD,
                DOWN);
        });
    }
}

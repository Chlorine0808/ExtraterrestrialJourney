package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.RankData;
import chlorine.etjourney.content.rank.RankTasks;
import chlorine.etjourney.world.wind.WindWorld;
import chlorine.etjourney.world.wind.model.Wind;

/** Every 10 seconds, the air over a target within 24 blocks is warned, then pushed down for 3 seconds. */
public final class DowndraftAbility implements RankAbility {

    public static final String ID = "downdraft";
    static final int COOLDOWN = 200, WARNING = 20, LASTS = 60;
    static final double REACH = 24, HALF_WIDTH = 1.5, BELOW = 1, ABOVE = 12, LIFT = 0.08, MAX_SPEED = 1.0;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, REACH);
        if (player == null) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        RankData.markUsed(self, ID, now);
        Warning.at(self.worldObj, "cloud", player.posX, player.posY + 3, player.posZ, WARNING, () -> {
            if (self.isEntityAlive() && player.isEntityAlive() && player.worldObj == self.worldObj)
                blow(player.dimension, player.posX, player.posY, player.posZ);
        });
    }

    /** Puts a downward wind around the spot and takes it away again after LASTS ticks. */
    private static void blow(int dimension, double x, double y, double z) {
        int id = WindWorld.SERVER.add(
            dimension,
            new Wind(
                x - HALF_WIDTH,
                y - BELOW,
                z - HALF_WIDTH,
                x + HALF_WIDTH,
                y + ABOVE,
                z + HALF_WIDTH,
                0,
                -1,
                0,
                LIFT,
                MAX_SPEED));
        RankTasks.later(LASTS, () -> WindWorld.SERVER.remove(dimension, id));
    }
}

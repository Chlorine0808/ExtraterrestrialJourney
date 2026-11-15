package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.rank.Rank;

/**
 * Each second, players near the mob are marked for 30 ticks; FamineEvents keeps a marked player from healing or eating.
 */
public final class FamineAbility implements RankAbility {

    public static final String ID = "famine";
    static final String KEY = "etjFamineUntil";
    static final int MARK = 30;
    private static final double REACH = 8;

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
        long until = self.worldObj.getTotalWorldTime() + MARK;
        boolean any = false;
        for (EntityPlayer p : Near.players(self, REACH)) {
            p.getEntityData()
                .setLong(KEY, until);
            any = true;
        }
        if (any) Offspring.puff(self, "smoke", 12);
    }

    /** True while the world time is before the player's mark runs out. */
    static boolean marked(long now, long until) {
        return now < until;
    }

    /** Whether the player is marked now. */
    public static boolean marked(EntityPlayer player) {
        return marked(
            player.worldObj.getTotalWorldTime(),
            player.getEntityData()
                .getLong(KEY));
    }
}

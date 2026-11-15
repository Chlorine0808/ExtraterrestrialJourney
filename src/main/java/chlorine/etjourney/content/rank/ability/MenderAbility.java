package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 4 seconds, the other hostile mobs within 8 blocks heal 4 health. */
public final class MenderAbility implements RankAbility {

    public static final String ID = "mender";
    static final int COOLDOWN = 80;
    private static final double REACH = 8, AMOUNT = 4;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        boolean healed = false;
        for (EntityLiving e : Near.hostiles(self, REACH)) {
            if (e.getHealth() >= e.getMaxHealth()) continue;
            e.heal((float) AMOUNT);
            Offspring.puff(e, "heart", 4);
            healed = true;
        }
        if (healed) RankData.markUsed(self, ID, now);
    }
}

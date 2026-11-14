package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.area.AreaTypes;
import chlorine.etjourney.content.area.Areas;
import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** While chasing a target on foot, the mob leaves a poison floor behind it every few seconds. */
public final class TrailAbility implements RankAbility {

    public static final String ID = "trail";
    static final int COOLDOWN = 60;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        if (self.getAttackTarget() == null || !self.onGround) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        if (Areas
            .placeOnGround(AreaTypes.RANK_TRAIL, self.worldObj, self, self.posX, self.boundingBox.minY + 0.5, self.posZ)
            != null) RankData.markUsed(self, ID, now);
    }
}

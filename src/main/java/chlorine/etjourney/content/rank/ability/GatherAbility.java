package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Every 6 seconds in a fight, hostile mobs within 24 blocks speed up and come to the mob. */
public final class GatherAbility implements RankAbility {

    public static final String ID = "gather";
    static final int COOLDOWN = 120, DURATION = 120;
    private static final double REACH = 24, NEAR = 4, SPEED = 1.2;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityLivingBase target = Near.target(self);
        if (target == null || !target.isEntityAlive()) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        for (EntityLiving e : Near.hostiles(self, REACH)) {
            if (self.getDistanceSqToEntity(e) <= NEAR * NEAR) continue;
            e.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, DURATION, 1));
            e.getNavigator()
                .tryMoveToEntityLiving(self, SPEED);
            if (e.getAttackTarget() == null) e.setAttackTarget(target);
        }
        RankData.markUsed(self, ID, now);
    }
}

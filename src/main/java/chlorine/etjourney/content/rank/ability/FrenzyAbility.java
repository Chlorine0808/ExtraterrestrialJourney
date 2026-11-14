package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.RankTasks;

/** After a melee hit lands, the mob hits again eight ticks later if the target is still in reach. */
public final class FrenzyAbility implements RankAbility {

    public static final String ID = "frenzy";
    static final int DELAY = 8;
    /** True while the extra hit is being made, so it does not bring another. */
    private static boolean extra;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onStrike(EntityLiving self, EntityLivingBase target, DamageSource source, float amount) {
        if (extra) return;
        RankTasks.later(DELAY, () -> hitAgain(self, target));
    }

    private static void hitAgain(EntityLiving self, EntityLivingBase target) {
        if (!self.isEntityAlive() || !target.isEntityAlive()
            || !inReach(self.getDistanceSqToEntity(target), self.width, target.width)) return;
        // The target is still in its hurt cooldown, which would swallow an equal hit.
        target.hurtResistantTime = 0;
        extra = true;
        try {
            self.attackEntityAsMob(target);
        } finally {
            extra = false;
        }
    }

    /** The vanilla melee reach: (2 * width) squared plus the target's width. */
    static boolean inReach(double distanceSq, float width, float targetWidth) {
        return distanceSq <= width * 2 * width * 2 + targetWidth;
    }
}

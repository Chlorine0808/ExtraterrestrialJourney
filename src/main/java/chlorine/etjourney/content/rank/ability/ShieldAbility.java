package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Absorption worth 30% of the maximum health, refilled once the mob goes eight seconds unhurt. */
public final class ShieldAbility implements RankAbility {

    public static final String ID = "shield";
    static final int REFILL_AFTER = 160;
    private static final float PART = 0.3f;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onGive(EntityLiving self) {
        self.setAbsorptionAmount(capacity(self));
    }

    @Override
    public void onJoin(EntityLiving self) {
        refillIfDue(self);
    }

    @Override
    public void onHurt(EntityLiving self, DamageSource source) {
        RankData.markUsed(self, ID, self.worldObj.getTotalWorldTime());
    }

    @Override
    public void onSecond(EntityLiving self) {
        refillIfDue(self);
    }

    private static void refillIfDue(EntityLiving self) {
        if (shouldRefill(
            self.worldObj.getTotalWorldTime(),
            RankData.lastUsed(self, ID),
            self.getAbsorptionAmount(),
            capacity(self))) self.setAbsorptionAmount(capacity(self));
    }

    private static float capacity(EntityLiving self) {
        return self.getMaxHealth() * PART;
    }

    static boolean shouldRefill(long now, long lastHurt, float absorption, float capacity) {
        return absorption < capacity && Cooldown.ready(now, lastHurt, REFILL_AFTER);
    }
}

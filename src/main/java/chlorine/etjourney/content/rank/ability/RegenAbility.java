package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;

/** Heals a part of the mob's maximum health each second. */
public final class RegenAbility implements RankAbility {

    public static final String ID = "regen";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        if (self.getHealth() < self.getMaxHealth()) self.heal(amount(self.getMaxHealth()));
    }

    /** 2% of the maximum health, at least 1. */
    static float amount(float maxHealth) {
        return Math.max(1f, maxHealth * 0.02f);
    }
}

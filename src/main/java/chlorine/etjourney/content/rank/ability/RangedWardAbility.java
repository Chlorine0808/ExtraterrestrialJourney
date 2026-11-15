package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;

/** Projectile damage is warded off and half of it heals the mob. */
public final class RangedWardAbility implements RankAbility {

    public static final String ID = "ranged_ward";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public boolean wards(EntityLiving self, DamageSource source, float amount) {
        if (!source.isProjectile()) return false;
        self.heal(amount / 2);
        Offspring.puff(self, "cloud", 8);
        return true;
    }
}

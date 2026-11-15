package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;

/** Magic damage is warded off and half of it heals the mob. */
public final class MagicWardAbility implements RankAbility {

    public static final String ID = "magic_ward";

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
        if (!source.isMagicDamage()) return false;
        self.heal(amount / 2);
        Offspring.puff(self, "witchMagic", 8);
        return true;
    }
}

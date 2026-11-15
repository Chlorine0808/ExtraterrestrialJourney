package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Rank;

/** Hits of one element are warded off and heal the mob by their amount. */
public final class AbsorbAbility implements RankAbility {

    public static final String ID = "absorb";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Rank least() {
        return Rank.EPIC;
    }

    @Override
    public void onGive(EntityLiving self) {
        HitElement.chooseIfUnset(self, ID);
    }

    @Override
    public boolean wards(EntityLiving self, DamageSource source, float amount) {
        if (!HitElement.matches(self, ID, source)) return false;
        self.heal(amount);
        Offspring.puff(self, "happyVillager", 8);
        return true;
    }
}

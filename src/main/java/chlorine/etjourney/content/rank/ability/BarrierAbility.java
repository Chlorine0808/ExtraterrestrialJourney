package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

/** Hits of one element are warded off. */
public final class BarrierAbility implements RankAbility {

    public static final String ID = "barrier";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onGive(EntityLiving self) {
        HitElement.chooseIfUnset(self, ID);
    }

    @Override
    public boolean wards(EntityLiving self, DamageSource source, float amount) {
        if (!HitElement.matches(self, ID, source)) return false;
        Offspring.puff(self, "enchantmenttable", 8);
        return true;
    }
}

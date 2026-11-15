package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

/** A melee hit on the mob hurts the attacker for 30% of the amount, at least one point. */
public final class ThornsAbility implements RankAbility {

    public static final String ID = "thorns";
    private static final float PART = 0.3f;
    /** True while the thorns hit is being made, so two thorned mobs do not trade it for ever. */
    private static boolean reflecting;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public float takes(EntityLiving self, DamageSource source, float amount) {
        if (reflecting || !(source.getEntity() instanceof EntityLivingBase)
            || source.getEntity() != source.getSourceOfDamage()) return amount;
        reflecting = true;
        try {
            source.getEntity()
                .attackEntityFrom(DamageSource.causeThornsDamage(self), reflected(amount));
        } finally {
            reflecting = false;
        }
        return amount;
    }

    static float reflected(float amount) {
        return Math.max(1f, amount * PART);
    }
}

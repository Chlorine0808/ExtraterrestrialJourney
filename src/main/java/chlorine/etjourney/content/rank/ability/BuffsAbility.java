package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/** Keeps Strength I, Resistance I and Fire Resistance on the mob. */
public final class BuffsAbility implements RankAbility {

    public static final String ID = "buffs";
    /** A little over a second, so the effect never lapses between refreshes. */
    static final int DURATION = 60;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        refresh(self, Potion.damageBoost.id, 0);
        refresh(self, Potion.resistance.id, 0);
        refresh(self, Potion.fireResistance.id, 0);
    }

    static void refresh(EntityLiving self, int potion, int amplifier) {
        self.addPotionEffect(new PotionEffect(potion, DURATION, amplifier, true));
    }
}

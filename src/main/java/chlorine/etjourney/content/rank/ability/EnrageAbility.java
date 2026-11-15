package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityLiving;
import net.minecraft.potion.Potion;

import chlorine.etjourney.content.rank.RankData;

/** Below 40% health the mob keeps Strength II and Speed I. */
public final class EnrageAbility implements RankAbility {

    public static final String ID = "enrage";
    private static final String ON = "on";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        if (!enraged(self.getHealth(), self.getMaxHealth())) {
            RankData.setSetting(self, ID, "");
            return;
        }
        BuffsAbility.refresh(self, Potion.damageBoost.id, 1);
        BuffsAbility.refresh(self, Potion.moveSpeed.id, 0);
        if (!ON.equals(RankData.setting(self, ID))) {
            RankData.setSetting(self, ID, ON);
            Offspring.puff(self, "angryVillager", 8);
        }
    }

    static boolean enraged(float health, float maxHealth) {
        return health < maxHealth * 0.4f;
    }
}

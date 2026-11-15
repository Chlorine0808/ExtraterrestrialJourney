package chlorine.etjourney.content.rank.ability;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.RankData;

/** Once, a killing blow leaves the mob at full health; the ability is then spent. */
public final class OneupAbility implements RankAbility {

    public static final String ID = "oneup";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean cheatsDeath(EntityLiving self, DamageSource source) {
        List<String> left = RankData.abilities(self);
        left.remove(ID);
        RankData.set(self, RankData.rank(self), left);
        self.setHealth(self.getMaxHealth());
        self.extinguish();
        Offspring.puff(self, "heart", 12);
        self.worldObj.playSoundAtEntity(self, "random.levelup", 1f, 1f);
        return true;
    }
}

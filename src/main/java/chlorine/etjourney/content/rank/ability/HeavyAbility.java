package chlorine.etjourney.content.rank.ability;

import java.util.UUID;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;

/** Hits 2.5 times as hard but holds back for two seconds after each landed hit. */
public final class HeavyAbility implements RankAbility {

    public static final String ID = "heavy";
    static final int REST = 40;
    private static final UUID MODIFIER = UUID.fromString("7b1e4c52-3d0a-4a7e-8f21-6c9d0e5a1b05");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onGive(EntityLiving self) {
        AttributeBoost.add(self, SharedMonsterAttributes.attackDamage, MODIFIER, 1.5);
    }

    @Override
    public boolean swings(EntityLiving self, EntityLivingBase target, DamageSource source) {
        return Cooldown.ready(self.worldObj.getTotalWorldTime(), RankData.lastUsed(self, ID), REST);
    }

    @Override
    public void onStrike(EntityLiving self, EntityLivingBase target, DamageSource source, float amount) {
        RankData.markUsed(self, ID, self.worldObj.getTotalWorldTime());
    }
}

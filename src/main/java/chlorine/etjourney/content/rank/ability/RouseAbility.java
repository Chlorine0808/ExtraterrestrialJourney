package chlorine.etjourney.content.rank.ability;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;

import chlorine.etjourney.content.rank.Cooldown;
import chlorine.etjourney.content.rank.RankData;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Every 5 seconds, unaware hostile mobs and wild wolves within 16 blocks are told who the mob is fighting. */
public final class RouseAbility implements RankAbility {

    public static final String ID = "rouse";
    static final int COOLDOWN = 100;
    private static final double SEE = 32, REACH = 16;
    private static final int ANGER = 400;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void onSecond(EntityLiving self) {
        EntityPlayer player = Near.playerTarget(self, SEE);
        if (player == null) return;
        long now = self.worldObj.getTotalWorldTime();
        if (!Cooldown.ready(now, RankData.lastUsed(self, ID), COOLDOWN)) return;
        for (Object o : self.worldObj
            .getEntitiesWithinAABB(EntityLiving.class, self.boundingBox.expand(REACH, REACH, REACH))) {
            EntityLiving e = (EntityLiving) o;
            if (e == self || !e.isEntityAlive() || self.getDistanceSqToEntity(e) > REACH * REACH) continue;
            if (RankData.role(e) == RankData.MINION) continue;
            boolean wolf = e instanceof EntityWolf && !((EntityWolf) e).isTamed();
            boolean unaware = e instanceof IMob && !(e instanceof IBossDisplayData) && e.getAttackTarget() == null;
            if (wolf || unaware) rouse(e, player);
        }
        RankData.markUsed(self, ID, now);
    }

    private static void rouse(EntityLiving e, EntityPlayer player) {
        e.setAttackTarget(player);
        e.setRevengeTarget(player);
        if (e instanceof EntityCreature) ((EntityCreature) e).setTarget(player);
        if (e instanceof EntityWolf) ((EntityWolf) e).setAngry(true);
        if (e instanceof EntityPigZombie)
            ReflectionHelper.setPrivateValue(EntityPigZombie.class, (EntityPigZombie) e, ANGER, "field_70837_d", "angerLevel");
        if (e instanceof EntityEnderman) ((EntityEnderman) e).setScreaming(true);
        Offspring.puff(e, "angryVillager", 6);
    }
}

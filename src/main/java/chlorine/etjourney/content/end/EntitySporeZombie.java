package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.registry.GameRegistry;

/** MYCELIA's melee mob: a zombie's tasks and sounds on a body a little tougher than an enderman's. */
public class EntitySporeZombie extends EntityMob {

    public EntitySporeZombie(World world) {
        super(world);
        getNavigator().setBreakDoors(false);
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        tasks.addTask(7, new EntityAIWander(this, 1.0D));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(44.0D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.26D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(0.2D);
    }

    @Override
    protected boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 4);
    }

    @Override
    protected String getLivingSound() {
        return "mob.zombie.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombie.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombie.death";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("mob.zombie.step", 0.15F, 1.0F);
    }

    @Override
    protected void dropFewItems(boolean byPlayer, int looting) {
        drop("mycelia_smallshroom", SporeZombieDrops.count(2, looting, rand));
        drop("mycelia_sprout", SporeZombieDrops.count(1, looting, rand));
    }

    private void drop(String name, int count) {
        Item item = GameRegistry.findItem(ModInfo.MODID, name);
        if (item != null && count > 0) dropItem(item, count);
    }
}

package chlorine.etjourney.content.end;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import chlorine.etjourney.content.worm.EntityWorm;
import chlorine.etjourney.content.worm.WormPhases.Medium;
import chlorine.etjourney.content.worm.WormSpec;

/** Solar's burrowing worm (a stand-in until the zone exists): breaks out of the ground and sets what it hits alight. */
public class EntityFlameWorm extends EntityWorm {

    static final WormSpec SPEC = WormSpec.builder(Medium.GROUND)
        .body(10, 0.9, 1.0F)
        .swim(0.35, 0.15)
        .strike(12, 30, 60)
        .leap(8, 4)
        .depth(5)
        .followRange(40)
        .build();
    private static final int BURN_SECONDS = 4;

    public EntityFlameWorm(World world) {
        super(world, SPEC);
        isImmuneToFire = true;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(120.0D);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(SPEC.followRange);
    }

    @Override
    public int getTotalArmorValue() {
        return Math.min(20, super.getTotalArmorValue() + 8);
    }

    @Override
    protected void onTouch(EntityPlayer player) {
        if (attackEntityAsMob(player)) player.setFire(BURN_SECONDS);
    }

    @Override
    protected void onLeap() {
        playSound("mob.ghast.fireball", 2.0F, 0.6F);
    }

    @Override
    protected void ambient() {
        if (rand.nextInt(3) == 0) {
            worldObj
                .spawnParticle("flame", posX + jitter(0.5), centreY() + jitter(0.5), posZ + jitter(0.5), 0, 0.02, 0);
        }
    }

    @Override
    protected String getLivingSound() {
        return "mob.blaze.breathe";
    }

    @Override
    protected String getHurtSound() {
        return "mob.blaze.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.blaze.death";
    }
}

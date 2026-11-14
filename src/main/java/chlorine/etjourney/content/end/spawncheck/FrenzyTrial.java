package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAITasks;

import chlorine.etjourney.content.rank.Rank;
import chlorine.etjourney.content.rank.ability.FrenzyAbility;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** frenzy: right after its first hit on a grounded player, the melee AI waits at most half as long as without it. */
final class FrenzyTrial extends AbilityTrial {

    private static final int SPELL = 100;
    private static final String[] ATTACK_TICK = { "field_75439_d", "attackTick" };

    private EntityLiving mob;
    private float last;
    private int plainWait = -1, frenzyWait = -1;

    FrenzyTrial() {
        super(FrenzyAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick == 1) mob = next(c, Rank.RARE);
        else if (tick == 1 + SPELL) {
            mob.setDead();
            mob = next(c, Rank.RARE, FrenzyAbility.ID);
        } else if (tick <= 1 + 2 * SPELL) {
            // The first drop in health is the first hit; what it brings (poison) comes later.
            if (c.player.getHealth() < last) {
                if (tick <= SPELL && plainWait < 0) plainWait = waitOf(mob);
                if (tick > SPELL && frenzyWait < 0) frenzyWait = waitOf(mob);
            }
        } else {
            c.verdict(
                frenzyWait >= 0 && frenzyWait <= 10 && plainWait > 10,
                name + ": the wait after a hit is " + frenzyWait + " ticks, " + plainWait + " without it");
            return true;
        }
        last = c.player.getHealth();
        return false;
    }

    private static int waitOf(EntityLiving mob) {
        for (Object o : mob.tasks.taskEntries) {
            Object action = ((EntityAITasks.EntityAITaskEntry) o).action;
            if (action instanceof EntityAIAttackOnCollide)
                return ReflectionHelper.<Integer, EntityAIAttackOnCollide>getPrivateValue(
                    EntityAIAttackOnCollide.class,
                    (EntityAIAttackOnCollide) action,
                    ATTACK_TICK);
        }
        return -1;
    }

    /** A mob of the rank next to the player, which is set down beside it; it hits for about one. */
    private static EntityLiving next(Ctx c, Rank rank, String... abilities) {
        c.player.capabilities.isFlying = false;
        c.player.sendPlayerAbilities();
        EntityLiving mob = c.spawn(rank, 1, 0, abilities);
        mob.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .setBaseValue(1);
        c.player.setPositionAndUpdate(mob.posX - 1, mob.posY, mob.posZ);
        c.player.setHealth(c.player.getMaxHealth());
        c.player.clearActivePotions();
        mob.setAttackTarget(c.player);
        return mob;
    }
}

package chlorine.etjourney.content.end.spawncheck;

import net.minecraft.entity.EntityLiving;

import chlorine.etjourney.content.rank.ability.GuardAbility;

/** guard: a hit from the front does nothing, a hit from behind hurts. */
final class GuardTrial extends AbilityTrial {

    GuardTrial() {
        super(GuardAbility.ID);
    }

    @Override
    boolean step(Ctx c, int tick) {
        if (tick != 1) return false;
        EntityLiving mob = c.spawn(4, 0, GuardAbility.ID);
        double toPlayer = Math.toDegrees(Math.atan2(-(c.player.posX - mob.posX), c.player.posZ - mob.posZ));
        face(mob, toPlayer);
        float before = mob.getHealth();
        mob.attackEntityFrom(c.byPlayer(), 4);
        c.verdict(
            mob.getHealth() == before,
            name + ": the front hit was warded (" + before + " -> " + mob.getHealth() + ")");
        face(mob, toPlayer + 180);
        mob.attackEntityFrom(c.byPlayer(), 4);
        c.verdict(
            mob.getHealth() < before,
            name + ": the hit from behind hurt (" + before + " -> " + mob.getHealth() + ")");
        return true;
    }

    private static void face(EntityLiving mob, double yaw) {
        mob.rotationYaw = (float) yaw;
        mob.renderYawOffset = (float) yaw;
        mob.rotationYawHead = (float) yaw;
    }
}
